package dev.mpa.client.data

import android.net.Uri

/**
 * Разбирает vless://uuid@host:port?security=reality&pbk=...&fp=...&sni=...&sid=...&flow=...#Name
 * Зеркало parseVlessUri() из electron/main/coreConfig.ts.
 */
object VlessParser {

    fun parse(rawUri: String): ServerProfile {
        val trimmed = rawUri.trim()
        if (!trimmed.lowercase().startsWith("vless://")) {
            throw IllegalArgumentException("Ссылка должна начинаться с vless://")
        }

        // Android Uri не понимает vless://, заменяем схему для парсинга
        val proxyUri = Uri.parse(trimmed.replaceFirst("vless://", "https://"))

        val uuid = proxyUri.userInfo
            ?: throw IllegalArgumentException("В ссылке отсутствует UUID пользователя")

        val address = proxyUri.host
            ?: throw IllegalArgumentException("Не удалось разобрать адрес сервера")

        val port = proxyUri.port.takeIf { it > 0 }
            ?: throw IllegalArgumentException("Не указан порт сервера")

        val security = proxyUri.getQueryParameter("security") ?: ""
        if (security != "reality") {
            throw IllegalArgumentException("Поддерживаются только конфигурации VLESS с Reality")
        }

        val publicKey = proxyUri.getQueryParameter("pbk")
            ?.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("В ссылке отсутствует публичный ключ Reality (pbk)")

        val fragment = proxyUri.fragment
        val name = if (!fragment.isNullOrEmpty()) {
            Uri.decode(fragment)
        } else {
            "$address:$port"
        }

        return ServerProfile(
            name = name,
            address = address,
            port = port,
            uuid = uuid,
            flow = proxyUri.getQueryParameter("flow") ?: "xtls-rprx-vision",
            network = "tcp",
            security = "reality",
            publicKey = publicKey,
            shortId = proxyUri.getQueryParameter("sid") ?: "",
            serverName = proxyUri.getQueryParameter("sni") ?: address,
            fingerprint = proxyUri.getQueryParameter("fp") ?: "chrome",
        )
    }
}
