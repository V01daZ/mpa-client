package dev.mpa.client.data

/**
 * Зеркало ConnectionState / ConnectionStatus из shared/types.ts.
 */
sealed class ConnectionStatus {
    data object Disconnected : ConnectionStatus()
    data class Connecting(val profileId: String) : ConnectionStatus()
    data class Connected(val profileId: String, val startedAt: Long = System.currentTimeMillis()) : ConnectionStatus()
    data class Disconnecting(val profileId: String?) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()

    /** Упрощённое строковое состояние для UI — совпадает с десктопом. */
    val stateName: String get() = when (this) {
        is Disconnected -> "disconnected"
        is Connecting -> "connecting"
        is Connected -> "connected"
        is Disconnecting -> "disconnecting"
        is Error -> "error"
    }

    val isBusy: Boolean get() = this is Connecting || this is Disconnecting
    val isConnected: Boolean get() = this is Connected
}
