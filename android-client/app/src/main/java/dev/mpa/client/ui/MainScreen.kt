package dev.mpa.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mpa.client.MainUiState
import dev.mpa.client.data.AppInfo
import dev.mpa.client.data.ConnectionStatus
import dev.mpa.client.data.DownloadState
import dev.mpa.client.data.ReleaseInfo
import dev.mpa.client.data.ServerProfile
import dev.mpa.client.data.SplitTunnelSettings
import dev.mpa.client.ui.components.AddServerDialog
import dev.mpa.client.ui.components.AnimatedBackground
import dev.mpa.client.ui.components.ConfirmDeleteDialog
import dev.mpa.client.ui.components.ConnectButton
import dev.mpa.client.ui.components.ServerCard
import dev.mpa.client.ui.components.UpdateBanner
import dev.mpa.client.ui.screens.QrScannerScreen
import dev.mpa.client.ui.screens.SplitTunnelScreen
import dev.mpa.client.ui.theme.Accent
import dev.mpa.client.ui.theme.AccentSoft
import dev.mpa.client.ui.theme.Border
import dev.mpa.client.ui.theme.Connected
import dev.mpa.client.ui.theme.ConnectedSoft
import dev.mpa.client.ui.theme.Error
import dev.mpa.client.ui.theme.ErrorSoft
import dev.mpa.client.ui.theme.Ink
import dev.mpa.client.ui.theme.SpaceGroteskFamily
import dev.mpa.client.ui.theme.Surface
import dev.mpa.client.ui.theme.TextMuted
import dev.mpa.client.ui.theme.TextPrimary

private enum class Screen { MAIN, QR_SCANNER, SPLIT_TUNNEL }

private data class StatusPill(
    val label: String,
    val bg: androidx.compose.ui.graphics.Color,
    val fg: androidx.compose.ui.graphics.Color,
)

private fun statusPill(status: ConnectionStatus) = when (status) {
    is ConnectionStatus.Disconnected  -> StatusPill("Отключено",      Surface,       TextMuted)
    is ConnectionStatus.Connecting    -> StatusPill("Подключение...", AccentSoft,    Accent)
    is ConnectionStatus.Connected     -> StatusPill("Подключено",     ConnectedSoft, Connected)
    is ConnectionStatus.Disconnecting -> StatusPill("Отключение...", AccentSoft,    Accent)
    is ConnectionStatus.Error         -> StatusPill("Ошибка",         ErrorSoft,     Error)
}

@Composable
fun MainScreen(
    uiState: MainUiState,
    installedApps: List<AppInfo>,
    onToggleConnection: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onRemoveProfile: (String) -> Unit,
    onAddProfile: (String) -> Unit,
    onDismissError: () -> Unit,
    onSplitTunnelChange: (SplitTunnelSettings) -> Unit,
    onOpenSplitTunnel: () -> Unit,
    onUpdateAction: () -> Unit,
    onDismissUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var screen by remember { mutableStateOf(Screen.MAIN) }
    var showAddDialog by remember { mutableStateOf(false) }
    var profileCountAtOpen by remember { mutableIntStateOf(0) }
    var pendingDeleteProfile by remember { mutableStateOf<ServerProfile?>(null) }
    var qrResult by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.profiles.size, uiState.isAddingProfile) {
        if (showAddDialog && !uiState.isAddingProfile && uiState.addProfileError == null
            && uiState.profiles.size > profileCountAtOpen) {
            showAddDialog = false
            qrResult = null
        }
    }

    when (screen) {
        Screen.QR_SCANNER -> {
            QrScannerScreen(
                onResult = { value ->
                    qrResult = value
                    screen = Screen.MAIN
                    showAddDialog = true
                    profileCountAtOpen = uiState.profiles.size
                },
                onBack = { screen = Screen.MAIN }
            )
            return
        }
        Screen.SPLIT_TUNNEL -> {
            SplitTunnelScreen(
                settings = uiState.splitTunnel,
                apps = installedApps,
                onSettingsChange = onSplitTunnelChange,
                onBack = { screen = Screen.MAIN }
            )
            return
        }
        Screen.MAIN -> { /* продолжаем */ }
    }

    val pill = statusPill(uiState.connectionStatus)
    val activeProfile = uiState.profiles.find { it.id == uiState.activeProfileId }
    val description = when {
        uiState.connectionStatus is ConnectionStatus.Error ->
            (uiState.connectionStatus as ConnectionStatus.Error).message
        activeProfile != null -> activeProfile.name
        else -> "Нет выбранного сервера"
    }

    // Показываем баннер если есть релиз и пользователь не скрыл,
    // или если уже скачано (не даём скрыть пока не нажал "Установить")
    val showBanner = uiState.availableRelease != null
        && (!uiState.updateDismissed || uiState.downloadState is DownloadState.Ready
            || uiState.downloadState is DownloadState.Downloading)

    Box(
        modifier = modifier.fillMaxSize().background(Ink)
    ) {
        AnimatedBackground()

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ─────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "MPA",
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onOpenSplitTunnel(); screen = Screen.SPLIT_TUNNEL }
                    ) {
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = "Выбор приложений",
                            tint = if (uiState.splitTunnel.enabled) Accent else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = pill.label,
                        color = pill.fg,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(pill.bg)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // ── Update banner ──────────────────────────────────────────────
            if (showBanner) {
                UpdateBanner(
                    release       = uiState.availableRelease!!,
                    downloadState = uiState.downloadState,
                    onUpdate      = onUpdateAction,
                    onDismiss     = onDismissUpdate,
                )
            }

            // ── Connect button ─────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                ConnectButton(
                    status = uiState.connectionStatus,
                    onToggle = onToggleConnection,
                )
                Text(
                    text = description,
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            // ── Servers section ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Ink.copy(alpha = 0.92f))
            ) {
                HorizontalDivider(color = Border)

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "СЕРВЕРЫ",
                        color = TextMuted,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        letterSpacing = 0.1.sp,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentSoft.copy(alpha = 0.5f))
                            .padding(horizontal = 4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                showAddDialog = true
                                profileCountAtOpen = uiState.profiles.size
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Accent)
                        }
                        Text(
                            text = "Добавить",
                            color = Accent,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }

                if (uiState.profiles.isEmpty()) {
                    Text(
                        text = "Серверов пока нет — нажми «Добавить» и вставь ссылку vless://, " +
                               "ссылку на подписку или ключ активации от бота.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 4.dp, bottom = 20.dp)
                    ) {
                        items(uiState.profiles, key = { it.id }) { profile ->
                            ServerCard(
                                profile  = profile,
                                isActive = profile.id == uiState.activeProfileId,
                                ping     = uiState.pings[profile.id],
                                onSelect = { onSelectProfile(profile.id) },
                                onRemove = { pendingDeleteProfile = profile },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddServerDialog(
            isLoading    = uiState.isAddingProfile,
            error        = uiState.addProfileError,
            initialInput = qrResult ?: "",
            onDismiss    = { showAddDialog = false; qrResult = null; onDismissError() },
            onAdd        = onAddProfile,
            onScanQr     = { showAddDialog = false; screen = Screen.QR_SCANNER },
        )
    }

    pendingDeleteProfile?.let { profile ->
        ConfirmDeleteDialog(
            profileName = profile.name,
            onConfirm   = { onRemoveProfile(profile.id); pendingDeleteProfile = null },
            onDismiss   = { pendingDeleteProfile = null }
        )
    }
}
