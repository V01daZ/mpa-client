package dev.mpa.client.data

/**
 * Настройки раздельного туннелирования.
 *
 * whitelistMode = true  → только выбранные приложения идут через VPN, остальные напрямую
 * whitelistMode = false → выбранные приложения идут напрямую, остальные через VPN
 */
data class SplitTunnelSettings(
    val enabled: Boolean = false,
    val whitelistMode: Boolean = false,   // false = blacklist (bypass VPN), true = whitelist (only VPN)
    val packageNames: Set<String> = emptySet(),
)
