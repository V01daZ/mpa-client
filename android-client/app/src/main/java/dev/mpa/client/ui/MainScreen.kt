package dev.mpa.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.mpa.client.MainUiState
import dev.mpa.client.data.AppInfo
import dev.mpa.client.data.ConnectionStatus
import dev.mpa.client.data.DownloadState
import dev.mpa.client.data.RuleSetState
import dev.mpa.client.data.ReleaseInfo
import dev.mpa.client.data.ServerProfile
import dev.mpa.client.data.SplitTunnelSettings
import dev.mpa.client.ui.components.AddServerDialog
import dev.mpa.client.ui.components.AnimatedBackground
import dev.mpa.client.ui.components.ConfirmDeleteDialog
import dev.mpa.client.ui.components.ConnectButton
import dev.mpa.client.ui.components.ServerCard
import dev.mpa.client.ui.components.RuleSetBanner
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
    var pendingRenameGroup by remember { mutableStateOf<Pair<String, String>?>(null) } // sourceUrl to currentGroupName

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

            // ── Rule-set banner (скачивание RU правил) ─────────────────────
            RuleSetBanner(state = uiState.ruleSetState)

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
                            .clickable {
                                showAddDialog = true
                                profileCountAtOpen = uiState.profiles.size
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Добавить",
                            tint = Accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Добавить",
                            color = Accent,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp),
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
                    val grouped = remember(uiState.profiles) {
                        uiState.profiles.groupBy { it.sourceUrl ?: "Manual" }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 4.dp, bottom = 20.dp)
                    ) {
                        grouped.forEach { (sourceUrl, profiles) ->
                            if (sourceUrl == "Manual" || profiles.size <= 1) {
                                items(profiles, key = { it.id }) { profile ->
                                    ServerCard(
                                        profile = profile,
                                        isActive = profile.id == uiState.activeProfileId,
                                        ping = uiState.pings[profile.id],
                                        onSelect = { onSelectProfile(profile.id) },
                                        onRemove = { pendingDeleteProfile = profile },
                                    )
                                }
                            } else {
                                val isExpanded = uiState.expandedGroups.contains(sourceUrl)
                                item(key = "group_$sourceUrl") {
                                    val currentGroupName = profiles.first().groupName ?: sourceUrl.substringAfter("://").substringBefore("/").takeIf { it.isNotBlank() } ?: "Подписка"
                                    GroupHeader(
                                        title = currentGroupName,
                                        count = profiles.size,
                                        isExpanded = isExpanded,
                                        isActive = profiles.any { it.id == uiState.activeProfileId },
                                        isLoading = uiState.isAddingProfile,
                                        onClick = { onToggleGroup(sourceUrl) },
                                        onRename = { pendingRenameGroup = sourceUrl to currentGroupName },
                                        onRefresh = { onRefreshGroup(sourceUrl) }
                                    )
                                }
                                if (isExpanded) {
                                    items(profiles, key = { it.id }) { profile ->
                                        ServerCard(
                                            profile = profile,
                                            isActive = profile.id == uiState.activeProfileId,
                                            ping = uiState.pings[profile.id],
                                            onSelect = { onSelectProfile(profile.id) },
                                            onRemove = { pendingDeleteProfile = profile },
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    }
                                }
                            }
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

    pendingRenameGroup?.let { (sourceUrl, currentName) ->
        RenameGroupDialog(
            currentName = currentName,
            onConfirm = { newName ->
                onRenameGroup(sourceUrl, newName)
                pendingRenameGroup = null
            },
            onDismiss = { pendingRenameGroup = null }
        )
    }
}

@Composable
private fun RenameGroupDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Surface)
                .border(1.dp, Border, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "Переименовать группу",
                color = TextPrimary,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Border,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Accent,
                    focusedContainerColor = Ink,
                    unfocusedContainerColor = Ink,
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отмена", color = TextMuted)
                }
                Button(
                    onClick = { onConfirm(name) },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сохранить", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    isActive: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(
                width = 1.dp,
                color = if (isActive) Connected.copy(alpha = 0.3f) else Border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isActive) ConnectedSoft else Border.copy(alpha = 0.2f))
        ) {
            if (isLoading && isActive) {
                 androidx.compose.material3.CircularProgressIndicator(
                     modifier = Modifier.size(16.dp),
                     strokeWidth = 2.dp,
                     color = Connected
                 )
            } else {
                Icon(
                    imageVector = if (isActive) Icons.Default.PhoneAndroid else Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isActive) Connected else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count серверов",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onRefresh,
                enabled = !isLoading,
                modifier = Modifier.size(24.dp)
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Accent
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onRename,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Переименовать",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextMuted
            )
        }
    }
}

