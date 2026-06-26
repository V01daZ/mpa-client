package dev.mpa.client.ui.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import dev.mpa.client.data.AppInfo
import dev.mpa.client.data.SplitTunnelSettings
import dev.mpa.client.ui.theme.Accent
import dev.mpa.client.ui.theme.AccentSoft
import dev.mpa.client.ui.theme.Border
import dev.mpa.client.ui.theme.Connected
import dev.mpa.client.ui.theme.ConnectedSoft
import dev.mpa.client.ui.theme.Ink
import dev.mpa.client.ui.theme.SpaceGroteskFamily
import dev.mpa.client.ui.theme.Surface
import dev.mpa.client.ui.theme.SurfaceHover
import dev.mpa.client.ui.theme.TextMuted
import dev.mpa.client.ui.theme.TextPrimary

@Composable
fun SplitTunnelScreen(
    settings: SplitTunnelSettings,
    apps: List<AppInfo>,
    onSettingsChange: (SplitTunnelSettings) -> Unit,
    onBack: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
            }
            Text(
                text = "Выбор приложений",
                color = TextPrimary,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // ── Enabled toggle ─────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column {
                Text(
                    text = "Раздельный туннель",
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
                Text(
                    text = "Какие приложения используют VPN",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            }
            Switch(
                checked = settings.enabled,
                onCheckedChange = { onSettingsChange(settings.copy(enabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Ink,
                    checkedTrackColor = Accent,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = Border,
                )
            )
        }

        HorizontalDivider(color = Border)

        if (settings.enabled) {
            // ── Mode selector ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "РЕЖИМ",
                    color = TextMuted,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    letterSpacing = 0.1.sp,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModeChip(
                        label = "Чёрный список",
                        subtitle = "Выбранные - напрямую",
                        selected = !settings.whitelistMode,
                        onClick = { onSettingsChange(settings.copy(whitelistMode = false)) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChip(
                        label = "Белый список",
                        subtitle = "Выбранные - через VPN",
                        selected = settings.whitelistMode,
                        onClick = { onSettingsChange(settings.copy(whitelistMode = true)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))
                val hint = if (settings.whitelistMode)
                    "Только отмеченные приложения идут через VPN, остальные — напрямую"
                else
                    "Отмеченные приложения обходят VPN и идут напрямую"
                Text(text = hint, color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
            }

            HorizontalDivider(color = Border)

            // ── Search ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск приложений...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Border,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Accent,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                ),
                singleLine = true,
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            )

            val selectedCount = settings.packageNames.size
            if (selectedCount > 0) {
                Text(
                    text = "Выбрано: $selectedCount",
                    color = Accent,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentSoft)
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // ── App list ────────────────────────────────────────────────────
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isSelected = app.packageName in settings.packageNames
                    AppRow(
                        app = app,
                        isSelected = isSelected,
                        onClick = {
                            val updated = if (isSelected)
                                settings.packageNames - app.packageName
                            else
                                settings.packageNames + app.packageName
                            onSettingsChange(settings.copy(packageNames = updated))
                        }
                    )
                    HorizontalDivider(color = Border.copy(alpha = 0.4f))
                }
            }
        } else {
            // Плейсхолдер когда выключено
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Включи раздельный туннель чтобы\nвыбрать приложения",
                    color = TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AccentSoft else Ink)
            .border(1.dp, if (selected) Accent else Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Accent else TextPrimary,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
        Text(
            text = subtitle,
            color = TextMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) ConnectedSoft.copy(alpha = 0.3f) else Ink)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Чекбокс
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) Connected else Border.copy(alpha = 0.3f))
                .border(1.dp, if (isSelected) Connected else Border, CircleShape)
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                color = TextPrimary,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.packageName,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (app.isSystem) {
            Text(
                text = "система",
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceHover)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    }
}
