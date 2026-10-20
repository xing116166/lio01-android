package com.xboard.app.vpn

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ProxyState { STOPPED, STARTING, CONNECTED, ERROR }

data class ProxyStatus(
    val state: ProxyState = ProxyState.STOPPED,
    val nodeName: String = "",
    val message: String = "",
)

/** Process-wide proxy state shared by the VPN service and the Compose UI. */
object ProxyController {

    private val _status = MutableStateFlow(ProxyStatus())
    val status: StateFlow<ProxyStatus> = _status.asStateFlow()

    fun start(context: Context, node: VpnNode) {
        val config = ProxyConfig.build(node)
        val intent = Intent(context, ProxyVpnService::class.java).apply {
            action = ProxyVpnService.ACTION_START
            putExtra(ProxyVpnService.EXTRA_CONFIG, config)
            putExtra(ProxyVpnService.EXTRA_NAME, node.name)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, ProxyVpnService::class.java).apply {
            action = ProxyVpnService.ACTION_STOP
        }
        context.startService(intent)
    }

    internal fun update(status: ProxyStatus) {
        _status.value = status
    }

    internal fun starting(name: String) = update(ProxyStatus(ProxyState.STARTING, name))
    internal fun connected(name: String) = update(ProxyStatus(ProxyState.CONNECTED, name))
    internal fun stopped() = update(ProxyStatus())
    internal fun error(name: String, message: String) =
        update(ProxyStatus(ProxyState.ERROR, name, message))
}
