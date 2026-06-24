package dev.mpa.client.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.mpa.client.MainActivity
import dev.mpa.client.R
import dev.mpa.client.data.ConnectionStatus
import dev.mpa.client.data.ServerProfile
import dev.mpa.client.data.SingBoxConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import libbox.BoxService
import libbox.InterfaceUpdateListener
import libbox.Libbox
import libbox.NetworkInterfaceIterator
import libbox.PlatformInterface
import libbox.TunOptions
import libbox.WIFIState

class MpaVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT    = "dev.mpa.client.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "dev.mpa.client.ACTION_DISCONNECT"
        const val EXTRA_PROFILE_JSON = "profile_json"

        private const val NOTIF_CHANNEL_ID = "mpa_vpn"
        private const val NOTIF_ID = 1

        private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
        val status: StateFlow<ConnectionStatus> = _status.asStateFlow()
    }

    inner class LocalBinder : Binder() {
        val service: MpaVpnService get() = this@MpaVpnService
    }

    private val binder = LocalBinder()
    private var boxService: BoxService? = null
    private var tunFd: Int = -1

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            disconnect()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() { disconnect(); super.onDestroy() }
    override fun onRevoke()  { disconnect() }

    // ── Public API ─────────────────────────────────────────────────────────

    fun connect(profile: ServerProfile) {
        if (_status.value.isBusy || _status.value.isConnected) disconnect()
        _status.value = ConnectionStatus.Connecting(profile.id)

        try {
            val configJson = SingBoxConfig.build(profile)
            android.util.Log.d("MpaVpnService", "Config: $configJson")

            tunFd = buildTunInterface()
            val svc = Libbox.newService(configJson, buildPlatformInterface(tunFd))
            svc.start()
            boxService = svc

            startForeground(NOTIF_ID, buildNotification(profile.name, connected = true))
            _status.value = ConnectionStatus.Connected(profile.id)

        } catch (e: Exception) {
            android.util.Log.e("MpaVpnService", "connect error: ${e.message}", e)
            _status.value = ConnectionStatus.Error(e.message ?: "Ошибка sing-box")
        }
    }

    fun disconnect() {
        val prevProfile = when (val s = _status.value) {
            is ConnectionStatus.Connected  -> s.profileId
            is ConnectionStatus.Connecting -> s.profileId
            else -> null
        }
        _status.value = ConnectionStatus.Disconnecting(prevProfile)
        try { boxService?.close() } catch (_: Exception) {}
        boxService = null
        tunFd = -1
        stopForeground(STOP_FOREGROUND_REMOVE)
        _status.value = ConnectionStatus.Disconnected
    }

    // ── TUN ────────────────────────────────────────────────────────────────
    //
    // auto_route=false в конфиге sing-box — значит sing-box НЕ добавляет маршруты сам.
    // Весь роутинг задаём здесь через VpnService.Builder.
    // Приватные подсети исключаем через addRoute с более специфичными префиксами
    // (Android не поддерживает excludeRoute, только addRoute — используем split tunneling).

    private fun buildTunInterface(): Int {
        val builder = Builder()
            .setSession("MPA")
            .addAddress(
                SingBoxConfig.TUN_ADDRESS_V4.substringBefore('/'),
                SingBoxConfig.TUN_ADDRESS_V4.substringAfter('/').toInt()
            )
            .addAddress(
                SingBoxConfig.TUN_ADDRESS_V6.substringBefore('/'),
                SingBoxConfig.TUN_ADDRESS_V6.substringAfter('/').toInt()
            )
            .addDnsServer("1.1.1.1")
            .setMtu(9000)
            // Само приложение идёт мимо TUN — иначе sing-box зациклится
            .addDisallowedApplication(packageName)

        // Маршрутизируем весь публичный трафик в TUN.
        // Приватные подсети добавляем через более специфичные маршруты — они
        // перекрывают дефолтный 0.0.0.0/0 на уровне ядра.
        // 
        // Публичный IPv4: всё кроме 10/8, 172.16/12, 192.168/16, 127/8, 169.254/16
        PUBLIC_IPV4_ROUTES.forEach { (addr, prefix) ->
            builder.addRoute(addr, prefix)
        }
        // Весь IPv6 в TUN (кроме link-local — они не роутятся)
        builder.addRoute("::", 0)

        val pfd = builder.establish()
            ?: throw IllegalStateException("VpnService.Builder.establish() вернул null — нет разрешения VPN")

        return pfd.detachFd()
    }

    // Split tunneling для IPv4: весь публичный трафик, без приватных подсетей.
    // Получено разбиением 0.0.0.0/0 с исключением RFC1918 + loopback + link-local.
    private val PUBLIC_IPV4_ROUTES = listOf(
        "1.0.0.0" to 8,
        "2.0.0.0" to 7,
        "4.0.0.0" to 6,
        "8.0.0.0" to 7,
        "11.0.0.0" to 8,
        "12.0.0.0" to 6,
        "16.0.0.0" to 4,
        "32.0.0.0" to 3,
        "64.0.0.0" to 2,
        "128.0.0.0" to 3,
        "160.0.0.0" to 5,
        "168.0.0.0" to 6,
        "170.0.0.0" to 7,
        "172.0.0.0" to 12,
        "172.32.0.0" to 11,
        "172.64.0.0" to 10,
        "172.128.0.0" to 9,
        "173.0.0.0" to 8,
        "174.0.0.0" to 7,
        "176.0.0.0" to 4,
        "192.0.0.0" to 9,
        "192.128.0.0" to 11,
        "192.160.0.0" to 13,
        "192.169.0.0" to 16,
        "192.170.0.0" to 15,
        "192.172.0.0" to 14,
        "192.176.0.0" to 12,
        "192.192.0.0" to 10,
        "193.0.0.0" to 8,
        "194.0.0.0" to 7,
        "196.0.0.0" to 6,
        "200.0.0.0" to 5,
        "208.0.0.0" to 4
    )

    // ── PlatformInterface ──────────────────────────────────────────────────

    private fun buildPlatformInterface(fd: Int): PlatformInterface = object : PlatformInterface {

        override fun openTun(options: TunOptions?): Int = fd

        override fun autoDetectInterfaceControl(fd: Int) { protect(fd) }

        override fun usePlatformAutoDetectInterfaceControl(): Boolean = true
        override fun usePlatformDefaultInterfaceMonitor(): Boolean    = true
        override fun usePlatformInterfaceGetter(): Boolean            = true

        override fun useProcFS(): Boolean             = false
        override fun includeAllNetworks(): Boolean    = false
        override fun underNetworkExtension(): Boolean = false

        override fun readWIFIState(): WIFIState?      = null
        override fun writeLog(message: String)        { android.util.Log.d("SingBox", message) }
        override fun clearDNSCache()                  {}

        override fun findConnectionOwner(
            ipProtocol: Int, sourceAddress: String, sourcePort: Int,
            destinationAddress: String, destinationPort: Int
        ): Int = -1

        override fun packageNameByUid(uid: Int): String         = ""
        override fun uidByPackageName(packageName: String): Int = -1
        override fun getInterfaces(): NetworkInterfaceIterator? = null
        override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {}
        override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {}
    }

    // ── Notifications ──────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(NOTIF_CHANNEL_ID, "MPA VPN", NotificationManager.IMPORTANCE_LOW)
                .also { it.setShowBadge(false) }
        )
    }

    private fun buildNotification(serverName: String, connected: Boolean): Notification {
        val mainPi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val disconnectPi = PendingIntent.getService(
            this, 0,
            Intent(this, MpaVpnService::class.java).apply { action = ACTION_DISCONNECT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setContentTitle(if (connected) "MPA подключено" else "MPA подключается...")
            .setContentText(serverName)
            .setOngoing(true)
            .setContentIntent(mainPi)
            .addAction(R.drawable.ic_vpn_key, "Отключить", disconnectPi)
            .build()
    }
}
