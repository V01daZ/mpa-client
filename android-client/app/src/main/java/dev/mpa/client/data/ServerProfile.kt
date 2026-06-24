package dev.mpa.client.data

import java.util.UUID

/**
 * Зеркало shared/types.ts::ServerProfile из десктопного клиента.
 * Все поля совпадают по смыслу — один и тот же VLESS+Reality профиль.
 */
data class ServerProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val port: Int,
    val uuid: String,
    val flow: String = "xtls-rprx-vision",
    val network: String = "tcp",
    val security: String = "reality",
    val publicKey: String,
    val shortId: String = "",
    val serverName: String,
    val fingerprint: String = "chrome",
    /** Как был добавлен профиль */
    val sourceType: SourceType = SourceType.VLESS,
    /** Для subscription / activation - URL подписки */
    val sourceUrl: String? = null,
    /** Для activation - короткий ключ */
    val activationKey: String? = null,
    /** Unix millis последнего обновления из подписки */
    val updatedAt: Long? = null,
)

enum class SourceType { VLESS, SUBSCRIPTION, ACTIVATION }
