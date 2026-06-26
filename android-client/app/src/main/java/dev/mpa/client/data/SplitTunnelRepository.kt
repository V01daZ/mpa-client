package dev.mpa.client.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.splitTunnelDataStore by preferencesDataStore("mpa_split_tunnel")

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
)

class SplitTunnelRepository(private val context: Context) {

    private object Keys {
        val ENABLED       = booleanPreferencesKey("split_tunnel_enabled")
        val WHITELIST_MODE = booleanPreferencesKey("whitelist_mode")
        val PACKAGES      = stringPreferencesKey("selected_packages")
    }

    val settingsFlow: Flow<SplitTunnelSettings> = context.splitTunnelDataStore.data.map { prefs ->
        SplitTunnelSettings(
            enabled = prefs[Keys.ENABLED] ?: false,
            whitelistMode = prefs[Keys.WHITELIST_MODE] ?: false,
            packageNames = prefs[Keys.PACKAGES]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet(),
        )
    }

    suspend fun save(settings: SplitTunnelSettings) {
        context.splitTunnelDataStore.edit { prefs ->
            prefs[Keys.ENABLED]        = settings.enabled
            prefs[Keys.WHITELIST_MODE] = settings.whitelistMode
            prefs[Keys.PACKAGES]       = settings.packageNames.joinToString(",")
        }
    }

    /** Возвращает список пользовательских приложений, отсортированных по имени. */
    fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        // Используем GET_META_DATA | GET_UNINSTALLED_PACKAGES для максимального охвата
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { info ->
                // Не показываем само MPA
                if (info.packageName == context.packageName) return@filter false

                // Проверка на системность
                val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                // 1. Если это не системное приложение (установлено пользователем) — всегда показываем
                if (!isSystem || isUpdatedSystem) return@filter true

                // 2. Для системных приложений показываем только те, у которых есть иконка в меню
                // или которые являются известными пользовательскими сервисами.
                val hasLaunchIntent = pm.getLaunchIntentForPackage(info.packageName) != null
                
                // Исключаем типичные фоновые сервисы по ключевым словам в ID, если они системные
                val isLikelyService = info.packageName.contains(".service") || 
                                     info.packageName.contains(".overlay") ||
                                     info.packageName.contains("android.auto_generated")

                hasLaunchIntent && !isLikelyService
            }
            .map { info ->
                AppInfo(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
