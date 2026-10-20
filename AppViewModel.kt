package com.xboard.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboard.app.data.ApiException
import com.xboard.app.data.AuthData
import com.xboard.app.data.InviteInfo
import com.xboard.app.data.Node
import com.xboard.app.data.Notice
import com.xboard.app.data.Order
import com.xboard.app.data.PanelConfig
import com.xboard.app.data.PaymentAction
import com.xboard.app.data.PaymentMethod
import com.xboard.app.data.Plan
import com.xboard.app.data.Session
import com.xboard.app.data.SessionStore
import com.xboard.app.data.Subscribe
import com.xboard.app.data.UnauthorizedException
import com.xboard.app.data.UserConfig
import com.xboard.app.data.UserInfo
import com.xboard.app.data.XboardApi
import com.xboard.app.util.isValidBaseUrl
import com.xboard.app.util.normalizeBaseUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class AppState(
    val ready: Boolean = false,
    val session: Session = Session(),
    val config: PanelConfig? = null,
    val userConfig: UserConfig? = null,
    val user: UserInfo? = null,
    val subscribe: Subscribe? = null,
    val plans: List<Plan> = emptyList(),
    val nodes: List<Node> = emptyList(),
    val orders: List<Order> = emptyList(),
    val notices: List<Notice> = emptyList(),
    val refreshing: Boolean = false,
) {
    val currencySymbol: String
        get() = userConfig?.currency_symbol?.takeIf { it.isNotBlank() } ?: "¥"

    val captchaEnabled: Boolean get() = (config?.is_captcha ?: 0) == 1
    val emailVerifyRequired: Boolean get() = (config?.is_email_verify ?: 0) == 1
    val inviteForce: Boolean get() = (config?.is_invite_force ?: 0) == 1

    val isActive: Boolean
        get() {
            val s = subscribe ?: return false
            val expired = s.expired_at
            val notExpired = expired == null || expired <= 0 || expired * 1000 > System.currentTimeMillis()
            val hasPlan = (s.plan_id ?: 0) > 0
            return hasPlan && notExpired && user?.banned != true
        }
}

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data object LoggedIn : UiEvent
    data object LoggedOut : UiEvent
    data object SessionExpired : UiEvent
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SessionStore(app)
    private val api = XboardApi()

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        val stored = store.current()
        val session = if (stored.baseUrl.isBlank()) {
            stored.copy(baseUrl = AppConfig.DEFAULT_BASE_URL).also { store.save(it) }
        } else {
            stored
        }
        applySession(session)
        _state.update { it.copy(session = session, ready = true) }

        if (session.baseUrl.isBlank()) return
        runCatching { api.guestConfig() }.onSuccess { config ->
            _state.update { it.copy(config = config) }
        }
        if (session.isLoggedIn) {
            try {
                refreshAll()
            } catch (e: UnauthorizedException) {
                handleExpired()
            } catch (_: Exception) {
                // Dashboard will surface refresh failures; stay on cached state.
            }
        }
    }

    private fun applySession(session: Session) {
        api.baseUrl = session.baseUrl
        api.authHeader = session.authHeader
    }

    // ------------------------------------------------------------- account

    suspend fun connectServer(rawUrl: String) {
        val url = normalizeBaseUrl(rawUrl)
        if (!isValidBaseUrl(url)) {
            throw ApiException("请输入有效的服务器地址，例如 https://example.com")
        }
        val previous = api.baseUrl
        api.baseUrl = url
        val config = try {
            api.guestConfig()
        } catch (e: Exception) {
            api.baseUrl = previous
            throw e
        }

        val session = state.value.session.copy(baseUrl = url, token = "", email = "")
        store.save(session)
        applySession(session)
        _state.update {
            it.copy(
                session = session,
                config = config,
                user = null,
                subscribe = null,
                plans = emptyList(),
                nodes = emptyList(),
                orders = emptyList(),
                notices = emptyList(),
            )
        }
    }

    suspend fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isEmpty()) {
            throw ApiException("请输入邮箱和密码")
        }
        val auth = api.login(trimmedEmail, password)
        completeAuthentication(auth, trimmedEmail)
    }

    suspend fun register(
        email: String,
        password: String,
        inviteCode: String?,
        emailCode: String?,
    ) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.length < 8) {
            throw ApiException("请输入邮箱，密码至少 8 位")
        }
        val auth = api.register(
            trimmedEmail,
            password,
            inviteCode?.trim()?.takeIf { it.isNotEmpty() },
            emailCode?.trim()?.takeIf { it.isNotEmpty() },
        )
        completeAuthentication(auth, trimmedEmail)
    }

    private suspend fun completeAuthentication(auth: AuthData, email: String) {
        val token = auth.auth_data.ifBlank { auth.token }
        if (token.isBlank()) throw ApiException("登录失败：服务器未返回登录凭证")
        val session = state.value.session.copy(token = token, email = email)
        store.save(session)
        applySession(session)
        _state.update { it.copy(session = session) }
        _events.emit(UiEvent.LoggedIn)
        try {
            refreshAll()
        } catch (e: UnauthorizedException) {
            handleExpired()
        }
    }

    suspend fun logout() {
        val session = state.value.session.copy(token = "", email = "")
        store.save(session)
        applySession(session)
        _state.update {
            AppState(ready = true, session = session, config = state.value.config)
        }
        _events.emit(UiEvent.LoggedOut)
    }

    fun handleExpired() {
        viewModelScope.launch {
            val session = state.value.session.copy(token = "", email = "")
            store.save(session)
            applySession(session)
            _state.update {
                it.copy(
                    session = session,
                    user = null,
                    subscribe = null,
                    plans = emptyList(),
                    nodes = emptyList(),
                    orders = emptyList(),
                )
            }
            _events.emit(UiEvent.SessionExpired)
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String) {
        if (newPassword.length < 8) throw ApiException("新密码至少 8 位")
        guarded { api.changePassword(oldPassword, newPassword) }
    }

    // ------------------------------------------------------------- dashboard

    suspend fun refreshAll() {
        if (state.value.session.token.isBlank()) return
        _state.update { it.copy(refreshing = true) }
        try {
            supervisorScope {
                val userTask = async { safe { api.userInfo() } }
                val subscribeTask = async { safe { api.subscribe() } }
                val plansTask = async { safe { api.plans() } }
                val nodesTask = async { safe { api.nodes() } }
                val ordersTask = async { safe { api.orders() } }
                val noticesTask = async { safe { api.notices().data } }
                val configTask = async { safe { api.userConfig() } }

                val user = userTask.await()
                val subscribe = subscribeTask.await()
                val plans = plansTask.await()
                val nodes = nodesTask.await()
                val orders = ordersTask.await()
                val notices = noticesTask.await()
                val config = configTask.await()

                val unauthorized = listOf(user, subscribe, plans, nodes, orders, notices, config)
                    .firstNotNullOfOrNull { result ->
                        (result.exceptionOrNull() as? UnauthorizedException)
                    }
                if (unauthorized != null) throw unauthorized

                _state.update { current ->
                    current.copy(
                        user = user.getOrNull() ?: current.user,
                        subscribe = subscribe.getOrNull() ?: current.subscribe,
                        plans = plans.getOrNull() ?: current.plans,
                        nodes = nodes.getOrNull() ?: current.nodes,
                        orders = orders.getOrNull() ?: current.orders,
                        notices = notices.getOrNull() ?: current.notices,
                        userConfig = config.getOrNull() ?: current.userConfig,
                    )
                }
            }
        } catch (e: UnauthorizedException) {
            handleExpired()
        } finally {
            _state.update { it.copy(refreshing = false) }
        }
    }

    suspend fun refreshUser() {
        guarded {
            val user = api.userInfo()
            val subscribe = api.subscribe()
            _state.update { it.copy(user = user, subscribe = subscribe) }
        }
    }

    suspend fun refreshOrders() {
        guarded {
            val orders = api.orders()
            _state.update { it.copy(orders = orders) }
        }
    }

    suspend fun loadNotices() {
        guarded {
            val notices = api.notices().data
            _state.update { it.copy(notices = notices) }
        }
    }

    // ---------------------------------------------------------------- orders

    suspend fun createOrder(planId: Int, period: String, couponCode: String?): String =
        guarded {
            api.createOrder(planId, period, couponCode?.trim()?.takeIf { it.isNotEmpty() })
        }

    suspend fun orderDetail(tradeNo: String): Order = guarded { api.orderDetail(tradeNo) }

    suspend fun paymentMethods(): List<PaymentMethod> = guarded { api.paymentMethods() }

    suspend fun checkout(tradeNo: String, methodId: Int?): PaymentAction =
        guarded { api.checkout(tradeNo, methodId) }

    suspend fun checkOrder(tradeNo: String): Int = guarded { api.checkOrder(tradeNo) }

    suspend fun cancelOrder(tradeNo: String) {
        guarded { api.cancelOrder(tradeNo) }
        refreshOrders()
    }

    // ---------------------------------------------------------------- invite

    suspend fun inviteInfo(): InviteInfo = guarded { api.inviteInfo() }

    suspend fun createInviteCode() {
        guarded { api.createInviteCode() }
    }

    // ----------------------------------------------------------------- misc

    suspend fun sendEmailVerifyCode(email: String) {
        guarded { api.sendEmailVerify(email.trim()) }
    }

    suspend fun resetPassword(email: String, code: String, password: String) {
        if (password.length < 8) throw ApiException("密码至少 8 位")
        guarded { api.forgetPassword(email.trim(), code.trim(), password) }
    }

    suspend fun updateTheme(mode: String) {
        val session = state.value.session.copy(theme = mode)
        store.save(session)
        _state.update { it.copy(session = session) }
    }

    // -------------------------------------------------------------- helpers

    private suspend fun <T> guarded(block: suspend () -> T): T = try {
        block()
    } catch (e: UnauthorizedException) {
        handleExpired()
        throw e
    }

    private suspend fun <T> safe(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
