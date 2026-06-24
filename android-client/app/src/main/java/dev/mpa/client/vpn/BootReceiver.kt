package dev.mpa.client.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Автозапуск VPN при загрузке устройства.
 * Аналог isAutostart-логики в electron/main/index.ts.
 *
 * Реальный запуск делегируется MpaVpnService через startForegroundService —
 * сам ресивер не может долго работать.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        // Запускаем сервис — он сам прочитает активный профиль и подключится
        val serviceIntent = Intent(context, MpaVpnService::class.java).apply {
            action = MpaVpnService.ACTION_CONNECT
            putExtra("autostart", true)
        }
        context.startForegroundService(serviceIntent)
    }
}
