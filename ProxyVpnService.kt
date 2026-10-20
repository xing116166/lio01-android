package com.xboard.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.xboard.app.MainActivity
import com.xboard.app.R
import io.nekohasekai.libbox.BoxService
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.net.NetworkInterface

/**
 * Android VpnService that runs sing-box in-process through libbox.
 *
 * TUN creation is delegated to [openTun] by libbox, which lets the Go core
 * own routing and DNS for the whole device.
 */
class ProxyVpnService : VpnService(), PlatformInterface {

    companion object {
        const val ACTION_START = "com.xboard.app.proxy.START"
        const val ACTION_STOP = "com.xboard.app.proxy.STOP"
        const val EXTRA_CONFIG = "config"
        const val EXTRA_NAME = "name"

        private const val CHANNEL_ID = "lio01_proxy"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "ProxyVpnService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var box: BoxService? = null
    private var tun: ParcelFileDescriptor? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var interfaceListener: InterfaceUpdateListener? = null
    private var currentNodeName: String = ""

    private val connectivity: ConnectivityManager
        get() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopProxy()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val config = intent?.getStringExtra(EXTRA_CONFIG)
                currentNodeName = intent?.getStringExtra(EXTRA_NAME).orEmpty()
                if (config.isNullOrBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForegroundNotification()
                ProxyController.starting(currentNodeName)
                scope.launch { startProxy(config, currentNodeName) }
            }
        }
        return START_STICKY
    }

    private fun startProxy(config: String, nodeName: String) {
        try {
            Libbox.setup(
                filesDir.absolutePath,
                filesDir.absolutePath,
                cacheDir.absolutePath,
                false,
            )
            val service = Libbox.newService(config, this)
            box = service
            service.start()
            ProxyController.connected(nodeName)
            updateNotification("已连接 · $nodeName")
        } catch (error: Throwable) {
            Log.e(TAG, "failed to start sing-box", error)
            stopProxy()
            ProxyController.error(nodeName, error.message ?: "代理启动失败")
            stopSelf()
        }
    }

    private fun stopProxy() {
        runCatching { box?.close() }
        box = null
        runCatching { tun?.close() }
        tun = null
        networkCallback?.let { callback ->
            runCatching { connectivity.unregisterNetworkCallback(callback) }
        }
        networkCallback = null
        interfaceListener = null
        ProxyController.stopped()
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
    }

    override fun onRevoke() {
        stopProxy()
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        stopProxy()
        super.onDestroy()
    }

    // ------------------------------------------------------------- VPN / TUN

    override fun openTun(options: TunOptions): Int {
        val builder = Builder()
            .setSession("lio01")
            .setMtu(options.mtu)

        // Keep this app's own panel / subscription traffic outside the tunnel.
        runCatching { builder.addDisallowedApplication(packageName) }

        val inet4 = options.inet4Address
        while (inet4.hasNext()) {
            val prefix = inet4.next()
            builder.addAddress(prefix.address(), prefix.prefix())
        }
        val inet6 = options.inet6Address
        while (inet6.hasNext()) {
            val prefix = inet6.next()
            runCatching { builder.addAddress(prefix.address(), prefix.prefix()) }
        }

        val dns = options.dnsServerAddress
        if (!dns.isNullOrBlank()) {
            runCatching { builder.addDnsServer(dns) }
        } else {
            builder.addDnsServer("1.1.1.1")
        }

        if (options.autoRoute) {
            builder.addRoute("0.0.0.0", 0)
            builder.addRoute("::", 0)
        }

        val include = options.includePackage
        while (include.hasNext()) {
            runCatching { builder.addAllowedApplication(include.next()) }
        }
        val exclude = options.excludePackage
        while (exclude.hasNext()) {
            runCatching { builder.addDisallowedApplication(exclude.next()) }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        val descriptor = builder.establish() ?: throw IllegalStateException("无法建立 VPN 接口")
        tun?.close()
        tun = descriptor
        return descriptor.fd
    }

    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

    override fun autoDetectInterfaceControl(fd: Int) {
        protect(fd)
    }

    override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String,
        sourcePort: Int,
        destinationAddress: String,
        destinationPort: Int,
    ): Int = -1

    override fun packageNameByUid(uid: Int): String =
        packageManager.getPackagesForUid(uid)?.firstOrNull().orEmpty()

    override fun uidByPackageName(packageName: String): Int = try {
        packageManager.getApplicationInfo(packageName, 0).uid
    } catch (error: Exception) {
        -1
    }

    override fun usePlatformDefaultInterfaceMonitor(): Boolean = true

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        interfaceListener = listener
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = reportInterface(network)
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
                reportInterface(network)

            override fun onLost(network: Network) {
                listener.updateDefaultInterface("", 0)
            }
        }
        networkCallback = callback
        runCatching { connectivity.registerDefaultNetworkCallback(callback) }
    }

    private fun reportInterface(network: Network) {
        val listener = interfaceListener ?: return
        val properties = connectivity.getLinkProperties(network) ?: return
        val name = properties.interfaceName ?: return
        val index = runCatching { NetworkInterface.getByName(name)?.index ?: 0 }.getOrDefault(0)
        listener.updateDefaultInterface(name, index)
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        networkCallback?.let { callback ->
            runCatching { connectivity.unregisterNetworkCallback(callback) }
        }
        networkCallback = null
        interfaceListener = null
    }

    override fun usePlatformInterfaceGetter(): Boolean = false

    override fun getInterfaces(): NetworkInterfaceIterator = EmptyInterfaceIterator

    override fun underNetworkExtension(): Boolean = false

    override fun includeAllNetworks(): Boolean = false

    override fun readWIFIState(): WIFIState? = null

    override fun clearDNSCache() = Unit

    override fun writeLog(message: String) {
        Log.d("libbox", message)
    }

    private object EmptyInterfaceIterator : NetworkInterfaceIterator {
        override fun hasNext(): Boolean = false
        override fun next(): io.nekohasekai.libbox.NetworkInterface =
            throw NoSuchElementException()
    }

    // ---------------------------------------------------------- notification

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "代理连接", NotificationManager.IMPORTANCE_LOW).apply {
                description = "显示代理连接状态"
                setShowBadge(false)
            },
        )
    }

    private fun buildNotification(text: String): Notification {
        createChannel()
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ProxyVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vpn)
            .setContentTitle("lio01 代理")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, "断开", stopIntent)
            .build()
    }

    private fun startForegroundNotification() {
        val notification = buildNotification("正在连接…")
        val type = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
