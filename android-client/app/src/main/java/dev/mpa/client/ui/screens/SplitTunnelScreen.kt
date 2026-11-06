package dev.mpa.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mpa.client.data.AppInfo
import dev.mpa.client.data.SplitTunnelSettings
import dev.mpa.client.ui.theme.Board
import dev.mpa.client.ui.theme.BoardRaised
import dev.mpa.client.ui.theme.Ground
import dev.mpa.client.ui.theme.MonoData
import dev.mpa.client.ui.theme.OnBoard
import dev.mpa.client.ui.theme.OnBoardMuted
import dev.mpa.client.ui.theme.Paper
import dev.mpa.client.ui.theme.PaperInk
import dev.mpa.client.ui.theme.Rule
import dev.mpa.client.ui.theme.Signal

/** Иконка приложения: грузится в фоне, пока нет — пустое место того же размера. */
@Composable
private fun AppIcon(packageName: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val icon by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, packageName) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(96, 96)
                    .asImageBitmap()
            }.getOrNull()
        }
    }
    val m = Modifier.size(40.dp)
    icon?.let { androidx.compose.foundation.Image(it, contentDescription = null, modifier = m) }
        ?: Box(m)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTunnelScreen(
    settings: SplitTunnelSettings,
    apps: List<AppInfo>,
    onSettingsChange: (SplitTunnelSettings) -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    // Отмеченные при открытии — сверху; во время выбора список не прыгает
    val pinned = remember { settings.packageNames }
    val filtered = remember(apps, query) {
        apps.filter {
            query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true)
        }.sortedByDescending { it.packageName in pinned }
    }

    Scaffold(
        containerColor = Ground,
        topBar = {
            TopAppBar(
                title = { Text("Приложения") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Ground, titleContentColor = OnBoard, navigationIconContentColor = OnBoard,
                ),
            )
        },
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding() + 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "toggle") {
                ListItem(
                    headlineContent = { Text("Выбирать приложения") },
                    supportingContent = { Text("По умолчанию через VPN идёт всё") },
                    trailingContent = {
                        Switch(
                            checked = settings.enabled,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PaperInk, checkedTrackColor = Paper,
                                uncheckedThumbColor = OnBoardMuted, uncheckedTrackColor = Board, uncheckedBorderColor = Rule,
                            ),
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Ground, headlineColor = OnBoard, supportingColor = OnBoardMuted,
                    ),
                    modifier = Modifier.clickable(role = Role.Switch) {
                        onSettingsChange(settings.copy(enabled = !settings.enabled))
                    },
                )
            }

            if (!settings.enabled) {
                item(key = "off") {
                    Text(
                        "Включи, чтобы выбрать, какие приложения пускать мимо VPN или только через него.",
                        color = OnBoardMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                return@LazyColumn
            }

            item(key = "mode") {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val modes = listOf(false to "Мимо VPN", true to "Только через VPN")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { i, (whitelist, label) ->
                            SegmentedButton(
                                selected = settings.whitelistMode == whitelist,
                                onClick = { onSettingsChange(settings.copy(whitelistMode = whitelist)) },
                                shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = Paper, activeContentColor = PaperInk,
                                    inactiveContainerColor = Ground, inactiveContentColor = OnBoard,
                                    activeBorderColor = Paper, inactiveBorderColor = Rule,
                                ),
                            ) { Text(label) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (settings.whitelistMode) "Через VPN пойдут только отмеченные приложения. Остальные — напрямую."
                        else "Отмеченные приложения пойдут напрямую. Остальные — через VPN.",
                        color = OnBoardMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Найти приложение") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Paper, unfocusedBorderColor = Rule, cursorColor = Paper,
                        focusedContainerColor = Board, unfocusedContainerColor = Board,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (settings.packageNames.isNotEmpty()) {
                item(key = "count") {
                    Text(
                        "Отмечено: ${settings.packageNames.size}",
                        color = Signal,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            if (apps.isEmpty()) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Paper)
                    }
                }
            } else if (filtered.isEmpty()) {
                item(key = "nothing") {
                    Text(
                        "Ничего не нашлось по «$query»",
                        color = OnBoardMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            items(filtered, key = { it.packageName }) { app ->
                val checked = app.packageName in settings.packageNames
                ListItem(
                    headlineContent = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { AppIcon(app.packageName) },
                    trailingContent = {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = Paper, checkmarkColor = PaperInk, uncheckedColor = OnBoardMuted),
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (checked) BoardRaised else Ground,
                        headlineColor = OnBoard, supportingColor = OnBoardMuted, overlineColor = OnBoardMuted,
                    ),
                    modifier = Modifier.animateItem().clickable(role = Role.Checkbox) {
                        val updated = if (checked) settings.packageNames - app.packageName
                        else settings.packageNames + app.packageName
                        onSettingsChange(settings.copy(packageNames = updated))
                    },
                )
            }
        }
    }
}
