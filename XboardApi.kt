package com.xboard.app.data

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.HttpUrl

open class XboardException(message: String) : Exception(message)

class ApiException(message: String, val statusCode: Int = 0) : XboardException(message)

class UnauthorizedException(message: String = "登录已过期，请重新登录") : XboardException(message)

class NetworkException(message: String) : XboardException(message)

/**
 * Small typed client for the Xboard / V2Board compatible REST API.
 *
 * The API base is `<panel>/api/v1`. Authentication uses a Sanctum bearer token
 * returned by `passport/auth/login` in the `auth_data` field.
 */
class XboardApi {

    @Volatile
    var baseUrl: String = ""

    @Volatile
    var authHeader: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    // ---------------------------------------------------------------- endpoints

    suspend fun guestConfig(): PanelConfig = getData("/guest/comm/config", PanelConfig.serializer())

    suspend fun userConfig(): UserConfig = getData("/user/comm/config", UserConfig.serializer())

    suspend fun login(email: String, password: String): AuthData =
        postData(
            "/passport/auth/login",
            body("email" to email, "password" to password),
            AuthData.serializer(),
        )

    suspend fun register(
        email: String,
        password: String,
        inviteCode: String?,
        emailCode: String?,
    ): AuthData =
        postData(
            "/passport/auth/register",
            body(
                "email" to email,
                "password" to password,
                "invite_code" to inviteCode,
                "email_code" to emailCode,
            ),
            AuthData.serializer(),
        )

    suspend fun forgetPassword(email: String, emailCode: String, password: String): Boolean =
        postData(
            "/passport/auth/forget",
            body("email" to email, "email_code" to emailCode, "password" to password),
            JsonElement.serializer(),
        ).let { true }

    suspend fun sendEmailVerify(email: String): Boolean =
        postData(
            "/passport/comm/sendEmailVerify",
            body("email" to email),
            JsonElement.serializer(),
        ).let { true }

    suspend fun userInfo(): UserInfo = getData("/user/info", UserInfo.serializer())

    suspend fun subscribe(): Subscribe = getData("/user/getSubscribe", Subscribe.serializer())

    suspend fun plans(): List<Plan> =
        getData("/user/plan/fetch", kotlinx.serialization.builtins.ListSerializer(Plan.serializer()))

    suspend fun nodes(): List<Node> =
        getData("/user/server/fetch", kotlinx.serialization.builtins.ListSerializer(Node.serializer()))

    suspend fun orders(): List<Order> =
        getData("/user/order/fetch", kotlinx.serialization.builtins.ListSerializer(Order.serializer()))

    suspend fun orderDetail(tradeNo: String): Order =
        getData(
            "/user/order/detail",
            Order.serializer(),
            query = mapOf("trade_no" to tradeNo),
        )

    /** [period] must be a legacy period key, e.g. `month_price` (validated by the panel). */
    suspend fun createOrder(planId: Int, period: String, couponCode: String?): String =
        postData(
            "/user/order/save",
            body("plan_id" to planId, "period" to period, "coupon_code" to couponCode),
            JsonElement.serializer(),
        ).jsonPrimitive.content

    suspend fun paymentMethods(): List<PaymentMethod> =
        getData(
            "/user/order/getPaymentMethod",
            kotlinx.serialization.builtins.ListSerializer(PaymentMethod.serializer()),
        )

    suspend fun checkout(tradeNo: String, methodId: Int?): PaymentAction {
        val text = execute(
            method = "POST",
            path = "/user/order/checkout",
            body = body("trade_no" to tradeNo, "method" to methodId),
        )
        val result = json.decodeFromString(CheckoutResult.serializer(), text)
        val payload = result.data
        val textPayload = payload?.asText().orEmpty()
        return when (result.type) {
            -1 -> PaymentAction.Completed
            0 -> {
                // A few payment plugins return HTML/form data instead of a redirect URL.
                if (textPayload.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) {
                    PaymentAction.OpenUrl(textPayload)
                } else {
                    PaymentAction.Raw(textPayload)
                }
            }
            1 -> PaymentAction.QrCode(textPayload)
            else -> {
                if (textPayload.isBlank()) PaymentAction.Unknown else PaymentAction.Raw(textPayload)
            }
        }
    }

    suspend fun checkOrder(tradeNo: String): Int =
        getData(
            "/user/order/check",
            JsonElement.serializer(),
            query = mapOf("trade_no" to tradeNo),
        ).jsonPrimitive.content.toIntOrNull() ?: 0

    suspend fun cancelOrder(tradeNo: String): Boolean =
        postData(
            "/user/order/cancel",
            body("trade_no" to tradeNo),
            JsonElement.serializer(),
        ).let { true }

    suspend fun notices(page: Int = 1): NoticePage {
        val text = execute(
            "GET",
            "/user/notice/fetch",
            query = mapOf("current" to page.toString()),
        )
        return try {
            json.decodeFromString(NoticePage.serializer(), text)
        } catch (e: Exception) {
            throw ApiException("公告数据解析失败")
        }
    }

    suspend fun inviteInfo(): InviteInfo = getData("/user/invite/fetch", InviteInfo.serializer())

    suspend fun createInviteCode(): Boolean =
        getData("/user/invite/save", JsonElement.serializer()).let { true }

    suspend fun changePassword(oldPassword: String, newPassword: String): Boolean =
        postData(
            "/user/changePassword",
            body("old_password" to oldPassword, "new_password" to newPassword),
            JsonElement.serializer(),
        ).let { true }

    /** Fetches the raw subscription payload (base64 share links by default). */
    suspend fun fetchSubscription(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "v2rayNG/1.9.0")
            .header("Accept", "*/*")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw ApiException("订阅获取失败（HTTP " + response.code + "）")
                }
                response.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            throw NetworkException("订阅获取失败，请检查网络")
        }
    }

    suspend fun trafficLog(): List<TrafficRecord> =
        getData(
            "/user/stat/getTrafficLog",
            kotlinx.serialization.builtins.ListSerializer(TrafficRecord.serializer()),
        )

    // ------------------------------------------------------------------ plumbing

    private suspend fun <T> getData(
        path: String,
        deserializer: KSerializer<T>,
        query: Map<String, String?> = emptyMap(),
    ): T {
        val text = execute("GET", path, query)
        return parseEnvelope(text, deserializer)
    }

    private suspend fun <T> postData(
        path: String,
        body: String,
        deserializer: KSerializer<T>,
    ): T {
        val text = execute("POST", path, body = body)
        return parseEnvelope(text, deserializer)
    }

    private fun <T> parseEnvelope(text: String, deserializer: KSerializer<T>): T {
        val envelope = try {
            json.decodeFromString(ApiEnvelope.serializer(deserializer), text)
        } catch (e: Exception) {
            throw ApiException("服务器返回了无法解析的数据")
        }
        if (envelope.status != null && envelope.status != "success") {
            throw ApiException(envelope.message ?: "请求失败")
        }
        return envelope.data ?: throw ApiException("服务器未返回数据")
    }

    private suspend fun execute(
        method: String,
        path: String,
        query: Map<String, String?> = emptyMap(),
        body: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val full = baseUrl.trimEnd('/') + "/api/v1" + path
        val url: HttpUrl = full.toHttpUrlOrNull()
            ?: throw ApiException("服务器地址无效，请检查后重试")

        val urlBuilder = url.newBuilder()
        query.forEach { (key, value) ->
            if (value != null) urlBuilder.addQueryParameter(key, value)
        }

        val requestBuilder = Request.Builder()
            .url(urlBuilder.build())
            .header("Accept", "application/json")
            .header("User-Agent", "lio01-Android/${com.xboard.app.BuildConfig.VERSION_NAME}")

        authHeader?.takeIf { it.isNotBlank() }?.let { requestBuilder.header("Authorization", it) }

        when (method) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(
                (body ?: "{}").toRequestBody("application/json; charset=utf-8".toMediaType()),
            )
            else -> throw ApiException("不支持的请求方式")
        }

        val response = try {
            client.newCall(requestBuilder.build()).execute()
        } catch (e: IOException) {
            throw NetworkException("网络连接失败，请检查网络或服务器地址")
        }

        response.use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val message = extractMessage(text) ?: "请求失败（HTTP ${resp.code}）"
                val looksUnauthorized = message.contains("登录") ||
                    message.contains("login", ignoreCase = true) ||
                    message.contains("Unauthenticated", ignoreCase = true)
                if (resp.code == 401 || (resp.code == 403 && looksUnauthorized)) {
                    throw UnauthorizedException(message)
                }
                throw ApiException(message, resp.code)
            }
            if (text.isBlank()) throw ApiException("服务器返回为空")
            text
        }
    }

    private fun extractMessage(text: String): String? = try {
        val obj = json.parseToJsonElement(text).jsonObject
        val message = obj["message"]?.jsonPrimitive?.contentOrNull
        if (!message.isNullOrBlank()) {
            message
        } else {
            obj["errors"]?.jsonObject?.values?.firstOrNull()
                ?.jsonPrimitive?.contentOrNull
        }
    } catch (e: Exception) {
        null
    }

    private fun body(vararg pairs: Pair<String, Any?>): String = buildJsonObject {
        pairs.forEach { (key, value) ->
            val element: JsonElement? = when (value) {
                null -> null
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is JsonElement -> value
                else -> JsonPrimitive(value.toString())
            }
            if (element != null && element !is JsonNull) put(key, element)
        }
    }.toString()

    private fun JsonElement.asText(): String = when (this) {
        is JsonPrimitive -> contentOrNull.orEmpty()
        else -> toString()
    }
}
