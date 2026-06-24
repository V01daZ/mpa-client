package dev.mpa.client.vpn

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.mpa.client.data.ProfileRepository
import dev.mpa.client.data.SourceType
import dev.mpa.client.data.SubscriptionResolver
import java.util.concurrent.TimeUnit

/**
 * Фоновое обновление профилей из подписки / ключа активации.
 *
 * Аналог hourly setInterval в electron/main/index.ts:
 *   - каждый час
 *   - только при наличии сети
 *   - обновляет только профили типа SUBSCRIPTION и ACTIVATION
 *   - если активный профиль обновился и VPN подключён - переподключается
 */
class SubscriptionRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = ProfileRepository(applicationContext)
        val profiles = repository.getProfiles()
        var hadError = false

        profiles.forEach { profile ->
            if (profile.sourceType == SourceType.VLESS) return@forEach

            try {
                val updated = SubscriptionResolver.refetch(profile) ?: return@forEach
                repository.updateProfile(profile.id, updated)

                // Если это активный профиль и VPN сейчас подключён -
                // переподключаемся с новым конфигом
                val activeId = repository.getActiveProfileId()
                val status = MpaVpnService.status.value
                if (profile.id == activeId && status.isConnected) {
                    val intent = android.content.Intent(
                        applicationContext,
                        MpaVpnService::class.java
                    ).apply {
                        action = MpaVpnService.ACTION_CONNECT
                        putExtra(MpaVpnService.EXTRA_PROFILE_JSON, updated.toString())
                    }
                    applicationContext.startForegroundService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.w("SubscriptionRefresh", "Failed to refresh ${profile.name}: ${e.message}")
                hadError = true
            }
        }

        return if (hadError) Result.retry() else Result.success()
    }

    companion object {
        private const val WORK_NAME = "mpa_subscription_refresh"

        /**
         * Регистрирует периодическую задачу обновления подписок.
         * Вызывается из MpaApplication при старте.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SubscriptionRefreshWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
