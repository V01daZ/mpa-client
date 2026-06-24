package dev.mpa.client.data

import android.util.Base64
import dev.mpa.client.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Резолвит строку ввода в профиль.
 * Зеркало resolveProfileInput() + fetchVlessFromSubscription() из
 * electron/main/subscription.ts.
 *
 * Поддерживаемые форматы:
 *   - vless://...           прямая ссылка
 *   - http(s)://...          подписка (base64 или plain vless-ссылки)
 *   - цифры                  ключ активации → /activate/<key> → подписка
 */
object SubscriptionResolver {

    data class ResolvedProfile(
        val profile: ServerProfile,
        val sourceType: SourceType,
        val sourceUrl: String? = null,
        val activationKey: String? = null,
    )

    suspend fun resolve(rawInput: String): ResolvedProfile = withContext(Dispatchers.IO) {
        val input = rawInput.trim()

        when {
            input.lowercase().startsWith("vless://") -> {
                val profile = VlessParser.parse(input)
                ResolvedProfile(profile, SourceType.VLESS)
            }

            input.matches(Regex("^https?://.*", RegexOption.IGNORE_CASE)) -> {
                val profile = fetchVlessFromSubscription(input)
                ResolvedProfile(profile, SourceType.SUBSCRIPTION, sourceUrl = input)
            }

            input.matches(Regex("^\\d+$")) -> {
                val subscriptionUrl = resolveActivationKey(input)
                val profile = fetchVlessFromSubscription(subscriptionUrl)
                ResolvedProfile(
                    profile,
                    SourceType.ACTIVATION,
                    sourceUrl = subscriptionUrl,
                    activationKey = input
                )
            }

            else -> throw IllegalArgumentException(
                "Не удалось распознать формат — вставь ссылку vless://, " +
                "ссылку на подписку или ключ активации"
            )
        }
    }

    suspend fun refetch(profile: ServerProfile): ServerProfile? = withContext(Dispatchers.IO) {
        when (profile.sourceType) {
            SourceType.ACTIVATION -> {
                val key = profile.activationKey ?: return@withContext null
                val subscriptionUrl = resolveActivationKey(key)
                val fresh = fetchVlessFromSubscription(subscriptionUrl)
                fresh.copy(
                    id = profile.id,
                    sourceType = SourceType.ACTIVATION,
                    activationKey = key,
                    sourceUrl = subscriptionUrl,
                    updatedAt = System.currentTimeMillis()
                )
            }
            SourceType.SUBSCRIPTION -> {
                val url = profile.sourceUrl ?: return@withContext null
                val fresh = fetchVlessFromSubscription(url)
                fresh.copy(
                    id = profile.id,
                    sourceType = SourceType.SUBSCRIPTION,
                    sourceUrl = url,
                    updatedAt = System.currentTimeMillis()
                )
            }
            SourceType.VLESS -> null
        }
    }

    private fun resolveActivationKey(key: String): String {
        val base = BuildConfig.MPA_ACTIVATION_API.trimEnd('/')
        if (base.isEmpty()) {
            throw IllegalStateException(
                "MPA_ACTIVATION_API не задан. Пересобери приложение с -PMPA_ACTIVATION_API=https://..."
            )
        }
        val (code, body) = httpGet("$base/activate/$key")
        when (code) {
            404 -> throw IllegalArgumentException("Ключ активации не найден")
            403 -> throw IllegalArgumentException("Ключ активации отозван")
        }
        if (code != 200) throw IllegalStateException("Ошибка сервера: HTTP $code")

        // Ожидаем JSON {"subscription_url": "..."}
        val match = Regex(""""subscription_url"\s*:\s*"([^"]+)"""").find(body)
            ?: throw IllegalStateException("Сервер не вернул subscription_url")
        return match.groupValues[1]
    }

    private fun fetchVlessFromSubscription(url: String): ServerProfile {
        val (code, body) = httpGet(url)
        if (code != 200) throw IllegalStateException("Ошибка загрузки подписки: HTTP $code")
        val decoded = decodeSubscription(body)
        val line = decoded.lines()
            .map { it.trim() }
            .firstOrNull { it.lowercase().startsWith("vless://") }
            ?: throw IllegalStateException("В подписке не найдено ни одной ссылки vless://")
        return VlessParser.parse(line)
    }

    private fun decodeSubscription(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.lowercase().contains("vless://")) return trimmed
        return try {
            String(Base64.decode(trimmed, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: Exception) {
            trimmed
        }
    }

    /** Минималистичный синхронный HTTP GET, возвращает (statusCode, body). */
    private fun httpGet(url: String): Pair<Int, String> {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "MPA-Android/1.0")
        }
        return try {
            val code = conn.responseCode
            val stream = if (code < 400) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.readText() ?: ""
            code to body
        } finally {
            conn.disconnect()
        }
    }
}
