package dev.mpa.client.data

import java.util.UUID

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
    val sourceType: SourceType = SourceType.VLESS,
    val sourceUrl: String? = null,
    val activationKey: String? = null,
    val updatedAt: Long? = null,
    val groupName: String? = null,
)

enum class SourceType { VLESS, SUBSCRIPTION, ACTIVATION }
