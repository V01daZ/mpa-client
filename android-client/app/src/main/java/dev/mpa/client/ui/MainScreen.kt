package dev.mpa.client.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mpa.client.MainUiState
import dev.mpa.client.data.AppInfo
import dev.mpa.client.data.ConnectionStatus
import dev.mpa.client.data.DownloadState
import dev.mpa.client.data.ServerProfile
import dev.mpa.client.data.SplitTunnelSettings
import dev.mpa.client.ui.components.AddServerDialog
import dev.mpa.client.ui.components.AddServerRow
import dev.mpa.client.ui.components.BoardHeader
import dev.mpa.client.ui.components.BoardRow
import dev.mpa.client.ui.components.BoardingPass
import dev.mpa.client.ui.components.ConfirmDeleteDialog
import dev.mpa.client.ui.components.EmptyBoard
import dev.mpa.client.ui.components.GroupHeader
import dev.mpa.client.ui.components.RenameGroupDialog
import dev.mpa.client.ui.components.RuleSetStrip
import dev.mpa.client.ui.components.UpdateBanner
import dev.mpa.client.ui.screens.QrScannerScreen
import dev.mpa.client.ui.screens.SplitTunnelScreen
import dev.mpa.client.ui.theme.Golos
import dev.mpa.client.ui.theme.Ground
import dev.mpa.client.ui.theme.OnBoard
import dev.mpa.client.ui.theme.OnBoardMuted
import dev.mpa.client.ui.theme.Signal
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private enum class Screen { MAIN, QR_SCANNER, SPLIT_TUNNEL }

private const val MANUAL = "Manual"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    installedApps: List<AppInfo>,
    onToggleConnection: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onRemoveProfile: (String) -> Unit,
    onAddProfile: (String, String?) -> Unit,
    onDismissError: () -> Unit,
    onSplitTunnelChange: (SplitTunnelSettings) -> Unit,
    onOpenSplitTunnel: () -> Unit,
    onToggleGroup: (String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onRefreshGroup: (String) -> Unit,
    onUpdateAction: () -> Unit,
    onDismissUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var screen by remember { mutableStateOf(Screen.MAIN) }
    var showAddDialog by remember { mutableStateOf(false) }
    var profileCountAtOpen by remember { mutableIntStateOf(0) }
    var pendingDeleteProfile by remember { mutableStateOf<ServerProfile?>(null) }
    var qrResult by remember { mutableStateOf<String?>(null) }
    var pendingRenameGroup by remember { mutableStateOf<Pair<String, String>?>(null) }
    val snackbar = remember { SnackbarHostState() }

    fun openAdd() { showAddDialog = true; profileCountAtOpen = uiState.profiles.size }

    // Системная кнопка «Назад» с подэкранов возвращает на главный, а не закрывает приложение
    BackHandler(enabled = screen != Screen.MAIN) { screen = Screen.MAIN }

    LaunchedEffect(uiState.profiles.size, uiState.isAddingProfile) {
        if (showAddDialog && !uiState.isAddingProfile && uiState.addProfileError == null
            && uiState.profiles.size > profileCountAtOpen) {
            showAddDialog = false
            qrResult = null
        }
    }

    // Ошибки обновления подписки раньше уходили в невидимый диалог — показываем снэкбаром
    LaunchedEffect(uiState.addProfileError, showAddDialog) {
        val err = uiState.addProfileError
        if (err != null && !showAddDialog) {
            snackbar.showSnackbar(err)
            onDismissError()
        }
    }

    when (screen) {
        Screen.QR_SCANNER -> {
            QrScannerScreen(
                onResult = { value ->
                    qrResult = value
                    screen = Screen.MAIN
                    openAdd()
                },
                onBack = { screen = Screen.MAIN },
            )
            return
        }
        Screen.SPLIT_TUNNEL -> {
            SplitTunnelScreen(
                settings = uiState.splitTunnel,
                apps = installedApps,
                onSettingsChange = onSplitTunnelChange,
                onBack = { screen = Screen.MAIN },
            )
            return
        }
        Screen.MAIN -> Unit
    }

    val activeProfile = uiState.profiles.find { it.id == uiState.activeProfileId }
    val isConnected = uiState.connectionStatus is ConnectionStatus.Connected
    val showBanner = uiState.availableRelease != null &&
        (!uiState.updateDismissed || uiState.downloadState is DownloadState.Ready ||
            uiState.downloadState is DownloadState.Downloading)

    // Подсветка изменений на табло: что пользователь «уже видел».
    // Новые серверы запоминаются молча; заметная смена пинга подсвечивается до касания табло.
    val seenPings = remember { mutableStateMapOf<String, Int>() }
    LaunchedEffect(uiState.pings) {
        uiState.pings.forEach { (id, ms) -> if (ms != null && id !in seenPings) seenPings[id] = ms }
    }
    fun isChanged(id: String): Boolean {
        val now = uiState.pings[id] ?: return false
        val seen = seenPings[id] ?: return false
        if ((now < 0) != (seen < 0)) return true
        return now >= 0 && kotlin.math.abs(now - seen) >= maxOf(40, seen / 3)
    }
    fun acknowledge() { uiState.pings.forEach { (id, ms) -> if (ms != null) seenPings[id] = ms } }

    // Табло: быстрые серверы выше, неизмеренные ниже, недоступные в конце
    fun byPing(list: List<ServerProfile>) = list.sortedBy { p ->
        when (val ms = uiState.pings[p.id]) {
            null -> Int.MAX_VALUE - 1
            -1 -> Int.MAX_VALUE
            else -> ms
        }
    }
    val groups = remember(uiState.profiles) { uiState.profiles.groupBy { it.sourceUrl ?: MANUAL } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Ground,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text("MPA", fontFamily = Golos, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 1.sp)
                },
                actions = {
                    IconButton(onClick = { onOpenSplitTunnel(); screen = Screen.SPLIT_TUNNEL }) {
                        Icon(
                            Icons.Default.AltRoute,
                            contentDescription = if (uiState.splitTunnel.enabled)
                                "Выбор приложений, включено" else "Выбор приложений",
                            tint = if (uiState.splitTunnel.enabled) Signal else OnBoardMuted,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ground, titleContentColor = OnBoard),
            )
        },
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "ruleset") { RuleSetStrip(uiState.ruleSetState) }

            if (showBanner) {
                item(key = "update") {
                    UpdateBanner(
                        release = uiState.availableRelease!!,
                        downloadState = uiState.downloadState,
                        onUpdate = onUpdateAction,
                        onDismiss = onDismissUpdate,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }

            item(key = "pass") {
                BoardingPass(
                    status = uiState.connectionStatus,
                    profile = activeProfile,
                    ping = activeProfile?.let { uiState.pings[it.id] },
                    onToggle = onToggleConnection,
                    onAddServer = ::openAdd,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item(key = "gap") { Spacer(Modifier.height(20.dp)) }

            if (uiState.profiles.isEmpty()) {
                item(key = "empty") {
                    EmptyBoard(onAdd = ::openAdd, onScan = { screen = Screen.QR_SCANNER })
                }
            } else {
                item(key = "header") { BoardHeader() }

                groups.forEach { (sourceUrl, profiles) ->
                    val grouped = sourceUrl != MANUAL && profiles.size > 1
                    val collapsed = grouped && sourceUrl in uiState.collapsedGroups
                    if (grouped) {
                        item(key = "group_$sourceUrl") {
                            // Старые версии называли группы "Profile N" — показываем по-русски
                            val title = profiles.first().groupName
                                ?.replace(Regex("""^Profile (\d+)$"""), "Подписка $1")
                                ?: sourceUrl.substringAfter("://").substringBefore("/").ifBlank { "Подписка" }
                            GroupHeader(
                                title = title,
                                count = profiles.size,
                                collapsed = collapsed,
                                refreshing = uiState.refreshingGroup == sourceUrl,
                                onToggle = { onToggleGroup(sourceUrl) },
                                onRefresh = { acknowledge(); onRefreshGroup(sourceUrl) },
                                onRename = { pendingRenameGroup = sourceUrl to title },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    if (!collapsed) {
                        items(byPing(profiles), key = { it.id }) { profile ->
                          androidx.compose.foundation.layout.Column(Modifier.animateItem()) {
                            BoardRow(
                                profile = profile,
                                isActive = profile.id == uiState.activeProfileId,
                                isConnected = isConnected && profile.id == uiState.activeProfileId,
                                ping = uiState.pings[profile.id],
                                changed = isChanged(profile.id),
                                onSelect = { acknowledge(); onSelectProfile(profile.id) },
                                onRemove = { pendingDeleteProfile = profile },
                            )
                            // Линейка табло между строками
                            androidx.compose.material3.HorizontalDivider(
                                color = dev.mpa.client.ui.theme.Rule,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                          }
                        }
                    }
                    if (grouped) item(key = "div_$sourceUrl") { Spacer(Modifier.height(12.dp)) }
                }
                item(key = "add") { AddServerRow(onAdd = ::openAdd) }
            }
        }
    }

    if (showAddDialog) {
        AddServerDialog(
            isLoading = uiState.isAddingProfile,
            error = uiState.addProfileError,
            initialInput = qrResult ?: "",
            onDismiss = { showAddDialog = false; qrResult = null; onDismissError() },
            onAdd = onAddProfile,
            onScanQr = { showAddDialog = false; screen = Screen.QR_SCANNER },
        )
    }

    pendingDeleteProfile?.let { profile ->
        ConfirmDeleteDialog(
            profileName = profile.name,
            onConfirm = { onRemoveProfile(profile.id); pendingDeleteProfile = null },
            onDismiss = { pendingDeleteProfile = null },
        )
    }

    pendingRenameGroup?.let { (sourceUrl, currentName) ->
        RenameGroupDialog(
            currentName = currentName,
            onConfirm = { newName -> onRenameGroup(sourceUrl, newName); pendingRenameGroup = null },
            onDismiss = { pendingRenameGroup = null },
        )
    }
}
