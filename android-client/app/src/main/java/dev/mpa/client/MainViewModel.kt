package dev.mpa.client

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.IBinder
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.mpa.client.data.ConnectionStatus
import dev.mpa.client.data.ProfileRepository
import dev.mpa.client.data.ServerProfile
import dev.mpa.client.data.SubscriptionResolver
import dev.mpa.client.vpn.MpaVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class MainUiState(
    val profiles: List<ServerProfile> = emptyList(),
    val activeProfileId: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val pings: Map<String, Int?> = emptyMap(),
    val addProfileError: String? = null,
    val isAddingProfile: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
    private val repository = ProfileRepository(context)

    // ── UI state ───────────────────────────────────────────────────────────

    private val _pings = MutableStateFlow<Map<String, Int?>>(emptyMap())
    private val _addError = MutableStateFlow<String?>(null)
    private val _isAdding = MutableStateFlow(false)

    // combine(f1,f2,f3,f4,f5) - 5 аргументов, внутри маппим в data class
    val uiState: StateFlow<MainUiState> = combine(
        repository.profilesFlow,
        repository.activeProfileIdFlow,
        MpaVpnService.status,
        _pings,
        combine(_addError, _isAdding) { err, adding -> err to adding },
    ) { profiles, activeId, status, pings, (addError, isAdding) ->
        MainUiState(
            profiles = profiles,
            activeProfileId = activeId,
            connectionStatus = status,
            pings = pings,
            addProfileError = addError,
            isAddingProfile = isAdding,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    // ── VPN service binding ────────────────────────────────────────────────

    private var vpnService: MpaVpnService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            vpnService = (binder as MpaVpnService.LocalBinder).service
        }
        override fun onServiceDisconnected(name: ComponentName) {
            vpnService = null
        }
    }

    fun bindService() {
        val intent = Intent(context, MpaVpnService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService() {
        runCatching { context.unbindService(serviceConnection) }
    }

    // ── Connection ─────────────────────────────────────────────────────────

    fun toggleConnection(permissionLauncher: ActivityResultLauncher<Intent>) {
        val status = uiState.value.connectionStatus
        if (status.isConnected || status.isBusy) {
            disconnect()
            return
        }
        val targetId = uiState.value.activeProfileId
            ?: uiState.value.profiles.firstOrNull()?.id
            ?: return

        val permIntent = VpnService.prepare(context)
        if (permIntent != null) {
            _pendingProfileId = targetId
            permissionLauncher.launch(permIntent)
        } else {
            connectToProfile(targetId)
        }
    }

    private var _pendingProfileId: String? = null

    fun onVpnPermissionResult(granted: Boolean) {
        if (!granted) return
        _pendingProfileId?.let { connectToProfile(it) }
        _pendingProfileId = null
    }

    private fun connectToProfile(profileId: String) {
        val profile = uiState.value.profiles.find { it.id == profileId } ?: return
        viewModelScope.launch {
            val service = getOrStartService()
            service.connect(profile)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            vpnService?.disconnect()
        }
    }

    private suspend fun getOrStartService(): MpaVpnService {
        vpnService?.let { return it }
        val intent = Intent(context, MpaVpnService::class.java)
        context.startForegroundService(intent)
        // Даём время на биндинг
        kotlinx.coroutines.delay(300)
        return vpnService ?: throw IllegalStateException("VPN-сервис не запустился")
    }

    // ── Profiles ───────────────────────────────────────────────────────────

    fun addProfileFromInput(input: String) {
        if (_isAdding.value) return
        _isAdding.value = true
        _addError.value = null

        viewModelScope.launch {
            try {
                val resolved = withContext(Dispatchers.IO) {
                    SubscriptionResolver.resolve(input)
                }
                val profile = resolved.profile.copy(
                    sourceType = resolved.sourceType,
                    sourceUrl = resolved.sourceUrl,
                    activationKey = resolved.activationKey,
                    updatedAt = System.currentTimeMillis(),
                )
                repository.addProfile(profile)
                refreshPingFor(profile)
            } catch (e: Exception) {
                _addError.value = e.message ?: "Неизвестная ошибка"
            } finally {
                _isAdding.value = false
            }
        }
    }

    fun clearAddError() { _addError.value = null }

    fun removeProfile(id: String) {
        viewModelScope.launch { repository.removeProfile(id) }
    }

    fun setActiveProfile(id: String) {
        viewModelScope.launch { repository.setActiveProfileId(id) }
    }

    // ── Ping ───────────────────────────────────────────────────────────────

    fun refreshAllPings() {
        viewModelScope.launch {
            uiState.value.profiles.forEach { refreshPingFor(it) }
        }
    }

    private fun refreshPingFor(profile: ServerProfile) {
        viewModelScope.launch {
            val ms = withContext(Dispatchers.IO) {
                tcpPing(profile.address, profile.port)
            }
            _pings.update { it + (profile.id to ms) }
        }
    }

    private fun tcpPing(host: String, port: Int, timeoutMs: Int = 2000): Int? {
        return try {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            (System.currentTimeMillis() - start).toInt()
        } catch (_: Exception) {
            null
        }
    }
}
