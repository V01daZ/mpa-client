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
import dev.mpa.client.data.AppInfo
import dev.mpa.client.data.ApkDownloader
import dev.mpa.client.data.ConnectionStatus
import dev.mpa.client.data.DownloadState
import dev.mpa.client.data.ProfileRepository
import dev.mpa.client.data.ReleaseInfo
import dev.mpa.client.data.ServerProfile
import dev.mpa.client.data.SplitTunnelRepository
import dev.mpa.client.data.SplitTunnelSettings
import dev.mpa.client.data.SubscriptionResolver
import dev.mpa.client.data.UpdateChecker
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
import dev.mpa.client.data.RuleSetDownloader
import dev.mpa.client.data.RuleSetState
import java.net.Socket

data class MainUiState(
    val profiles: List<ServerProfile> = emptyList(),
    val activeProfileId: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val pings: Map<String, Int?> = emptyMap(),
    val addProfileError: String? = null,
    val isAddingProfile: Boolean = false,
    val splitTunnel: SplitTunnelSettings = SplitTunnelSettings(),
    val availableRelease: ReleaseInfo? = null,
    val downloadState: DownloadState = DownloadState.Idle,
    val updateDismissed: Boolean = false,
    val expandedGroups: Set<String> = emptySet(),
    val ruleSetState: RuleSetState = RuleSetState.Idle,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
    private val repository      = ProfileRepository(context)
    private val splitTunnelRepo = SplitTunnelRepository(context)

    // ── Individual state flows ─────────────────────────────────────────────

    private val _pings           = MutableStateFlow<Map<String, Int?>>(emptyMap())
    private val _addError        = MutableStateFlow<String?>(null)
    private val _isAdding        = MutableStateFlow(false)
    private val _availableRelease = MutableStateFlow<ReleaseInfo?>(null)
    private val _downloadState   = MutableStateFlow<DownloadState>(DownloadState.Idle)
    private val _updateDismissed = MutableStateFlow(false)
    private val _expandedGroups  = MutableStateFlow<Set<String>>(emptySet())
    private val _ruleSetState    = MutableStateFlow<RuleSetState>(RuleSetState.Idle)

    // ── Промежуточные объединения чтобы не превышать arity combine() ───────

    // Группа 1: профили + активный + статус VPN
    private data class Group1(
        val profiles: List<ServerProfile>,
        val activeProfileId: String?,
        val connectionStatus: ConnectionStatus,
    )
    private val _group1 = combine(
        repository.profilesFlow,
        repository.activeProfileIdFlow,
        MpaVpnService.status,
    ) { profiles, activeId, status ->
        Group1(profiles, activeId, status)
    }

    // Группа 2: пинги + ошибки добавления
    private data class Group2(
        val pings: Map<String, Int?>,
        val addError: String?,
        val isAdding: Boolean,
    )
    private val _group2 = combine(
        _pings,
        _addError,
        _isAdding,
    ) { pings, err, adding ->
        Group2(pings, err, adding)
    }

    // Группа 3: split tunnel + обновления
    private data class Group3(
        val splitTunnel: SplitTunnelSettings,
        val availableRelease: ReleaseInfo?,
        val downloadState: DownloadState,
        val updateDismissed: Boolean,
        val expandedGroups: Set<String>,
        val ruleSetState: RuleSetState,
    )
    private val _group3 = combine(
        splitTunnelRepo.settingsFlow,
        _availableRelease,
        combine(_downloadState, _updateDismissed) { dl, dismissed -> dl to dismissed },
        combine(_expandedGroups, _ruleSetState) { expanded, rss -> expanded to rss },
    ) { st, rel, (dl, dismissed), (expanded, rss) ->
        Group3(st, rel, dl, dismissed, expanded, rss)
    }

    // ── Финальный uiState из трёх групп ───────────────────────────────────

    val uiState: StateFlow<MainUiState> = combine(
        _group1,
        _group2,
        _group3,
    ) { g1, g2, g3 ->
        MainUiState(
            profiles         = g1.profiles,
            activeProfileId  = g1.activeProfileId,
            connectionStatus = g1.connectionStatus,
            pings            = g2.pings,
            addProfileError  = g2.addError,
            isAddingProfile  = g2.isAdding,
            splitTunnel      = g3.splitTunnel,
            availableRelease = g3.availableRelease,
            downloadState    = g3.downloadState,
            updateDismissed  = g3.updateDismissed,
            expandedGroups   = g3.expandedGroups,
            ruleSetState     = g3.ruleSetState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    // ── Installed apps ─────────────────────────────────────────────────────

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps

    fun loadInstalledApps() {
        if (_installedApps.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = splitTunnelRepo.getInstalledApps()
        }
    }

    // ── Split tunnel ───────────────────────────────────────────────────────

    fun updateSplitTunnel(settings: SplitTunnelSettings) {
        viewModelScope.launch {
            splitTunnelRepo.save(settings)

            // Если VPN сейчас подключён — переподключаемся, чтобы применить новые правила
            val status = uiState.value.connectionStatus
            if (status.isConnected) {
                val activeId = uiState.value.activeProfileId ?: return@launch
                connectToProfile(activeId)
            }
        }
    }

    // ── Updates ────────────────────────────────────────────────────────────

    fun checkForUpdate() {
        viewModelScope.launch {
            val release = withContext(Dispatchers.IO) { UpdateChecker.checkForUpdate() }
            _availableRelease.value = release
        }
    }

    fun dismissUpdate() {
        _updateDismissed.value = true
    }

    fun onUpdateAction() {
        val release = _availableRelease.value ?: return
        when (val state = _downloadState.value) {
            is DownloadState.Ready       -> ApkDownloader.installApk(context, state.apkFile)
            is DownloadState.Downloading -> { /* ждём */ }
            else                         -> startDownload(release)
        }
    }

    private fun startDownload(release: ReleaseInfo) {
        val fileName = "mpa-android-v${release.versionName}-mrt${release.versionCode}.apk"
        viewModelScope.launch {
            ApkDownloader.download(context, release.apkUrl, fileName).collect { state ->
                _downloadState.value = state
            }
        }
    }

    // ── VPN service binding ────────────────────────────────────────────────

    private var vpnService: MpaVpnService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            vpnService = (binder as MpaVpnService.LocalBinder).service
        }
        override fun onServiceDisconnected(name: ComponentName) { vpnService = null }
    }

    // ── Rule sets ──────────────────────────────────────────────────────────

    fun downloadRuleSets() {
        viewModelScope.launch {
            RuleSetDownloader.ensureReady(context).collect { state ->
                _ruleSetState.value = state
            }
        }
    }

    fun bindService() {
        context.bindService(
            Intent(context, MpaVpnService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE,
        )
    }

    fun unbindService() { runCatching { context.unbindService(serviceConnection) } }

    // ── Connection ─────────────────────────────────────────────────────────

    fun toggleConnection(permissionLauncher: ActivityResultLauncher<Intent>) {
        val status = uiState.value.connectionStatus
        if (status.isConnected || status.isBusy) { disconnect(); return }

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
        val splitTunnel = uiState.value.splitTunnel
        viewModelScope.launch {
            val service = getOrStartService()
            service.connect(profile, splitTunnel)
        }
    }

    fun disconnect() { viewModelScope.launch { vpnService?.disconnect() } }

    private suspend fun getOrStartService(): MpaVpnService {
        vpnService?.let { return it }
        context.startForegroundService(Intent(context, MpaVpnService::class.java))
        kotlinx.coroutines.delay(300)
        return vpnService ?: throw IllegalStateException("VPN-сервис не запустился")
    }

    // ── Profiles ───────────────────────────────────────────────────────────

    fun addProfileFromInput(input: String, groupName: String? = null) {
        if (_isAdding.value) return
        _isAdding.value = true
        _addError.value = null
        viewModelScope.launch {
            try {
                val profiles = repository.getProfiles()
                val finalGroupName = if (groupName.isNullOrBlank()) {
                    generateNextProfileGroupName(profiles)
                } else {
                    groupName
                }

                val resolvedList = withContext(Dispatchers.IO) { SubscriptionResolver.resolve(input) }
                val profilesToAdd = resolvedList.map { resolved ->
                    resolved.profile.copy(
                        sourceType    = resolved.sourceType,
                        sourceUrl     = resolved.sourceUrl,
                        activationKey = resolved.activationKey,
                        updatedAt     = System.currentTimeMillis(),
                        groupName     = finalGroupName
                    )
                }
                repository.addProfiles(profilesToAdd)
                
                // Пингуем только новые
                profilesToAdd.forEach { refreshPingFor(it) }
            } catch (e: Exception) {
                _addError.value = e.message ?: "Неизвестная ошибка"
            } finally {
                _isAdding.value = false
            }
        }
    }

    private fun generateNextProfileGroupName(existingProfiles: List<ServerProfile>): String {
        val groups = existingProfiles.mapNotNull { it.groupName }.toSet()
        val regex = Regex("""Profile (\d+)""")
        val numbers = groups.mapNotNull { 
            regex.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() 
        }
        val nextNum = if (numbers.isEmpty()) 1 else (numbers.maxOrNull() ?: 0) + 1
        return "Profile $nextNum"
    }

    fun renameGroup(sourceUrl: String, newName: String) {
        viewModelScope.launch {
            repository.updateGroupName(sourceUrl, newName)
        }
    }

    fun forceRefreshGroup(sourceUrl: String) {
        if (_isAdding.value) return
        _isAdding.value = true
        _addError.value = null
        viewModelScope.launch {
            try {
                // Ищем любой профиль из этой группы, чтобы получить параметры источника
                val profiles = repository.getProfiles()
                val profile = profiles.find { it.sourceUrl == sourceUrl } ?: return@launch
                
                val freshProfiles = withContext(Dispatchers.IO) {
                    SubscriptionResolver.refetchAll(
                        sourceUrl,
                        profile.sourceType,
                        profile.activationKey
                    )
                }
                
                repository.addProfiles(freshProfiles)
                
                // Переподключаем VPN, если активный профиль был в этой группе
                val activeId = repository.getActiveProfileId()
                val updatedProfiles = repository.getProfiles()
                val activeProfile = updatedProfiles.find { it.id == activeId }
                
                if (activeProfile?.sourceUrl == sourceUrl && MpaVpnService.status.value.isConnected) {
                    connectToProfile(activeId!!)
                }
                
                freshProfiles.forEach { refreshPingFor(it) }
            } catch (e: Exception) {
                _addError.value = e.message ?: "Ошибка обновления"
            } finally {
                _isAdding.value = false
            }
        }
    }

    fun clearAddError()      { _addError.value = null }
    fun removeProfile(id: String) { viewModelScope.launch { repository.removeProfile(id) } }
    fun setActiveProfile(id: String) {
        viewModelScope.launch {
            repository.setActiveProfileId(id)
            
            // Если VPN подключён — перезапускаем на новом профиле
            val status = uiState.value.connectionStatus
            if (status.isConnected || status.isBusy) {
                connectToProfile(id)
            }
        }
    }

    fun toggleGroup(sourceUrl: String) {
        _expandedGroups.update { 
            if (it.contains(sourceUrl)) it - sourceUrl else it + sourceUrl 
        }
    }

    // ── Ping ───────────────────────────────────────────────────────────────

    fun refreshAllPings() {
        viewModelScope.launch { uiState.value.profiles.forEach { refreshPingFor(it) } }
    }

    private fun refreshPingFor(profile: ServerProfile) {
        viewModelScope.launch {
            val ms = withContext(Dispatchers.IO) { tcpPing(profile.address, profile.port) }
            _pings.update { it + (profile.id to ms) }
        }
    }

    private fun tcpPing(host: String, port: Int, timeoutMs: Int = 2000): Int? {
        return try {
            val start = System.currentTimeMillis()
            Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs) }
            (System.currentTimeMillis() - start).toInt()
        } catch (_: Exception) { null }
    }
}

