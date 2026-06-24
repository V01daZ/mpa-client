package dev.mpa.client

import android.app.Application
import dev.mpa.client.vpn.SubscriptionRefreshWorker
import libbox.Libbox

class MpaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Инициализируем sing-box runtime
        Libbox.setup(
            filesDir.absolutePath,   // basePath
            filesDir.absolutePath,   // workingPath
            cacheDir.absolutePath,   // tempPath
            false,                   // isTVOS
        )

        // Hourly обновление подписок
        SubscriptionRefreshWorker.schedule(this)
    }
}
