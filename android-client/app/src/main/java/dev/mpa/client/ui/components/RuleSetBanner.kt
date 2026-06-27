package dev.mpa.client.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mpa.client.data.RuleSetState
import dev.mpa.client.ui.theme.Accent
import dev.mpa.client.ui.theme.AccentSoft
import dev.mpa.client.ui.theme.Border
import dev.mpa.client.ui.theme.Connected
import dev.mpa.client.ui.theme.ConnectedSoft
import dev.mpa.client.ui.theme.TextMuted
import dev.mpa.client.ui.theme.TextPrimary

/**
 * Небольшой баннер-статус скачивания RU rule-set.
 * Показывается только пока идёт проверка/загрузка — исчезает когда Ready.
 */
@Composable
fun RuleSetBanner(
    state: RuleSetState,
    modifier: Modifier = Modifier,
) {
    val visible = state !is RuleSetState.Idle && state !is RuleSetState.Ready

    AnimatedVisibility(
        visible = visible,
        enter   = expandVertically(),
        exit    = shrinkVertically(),
        modifier = modifier,
    ) {
        val (icon, text, bg, fg) = when (state) {
            is RuleSetState.Checking    ->
                BannerData(null,  "Проверка RU правил маршрутизации...", AccentSoft, TextMuted)
            is RuleSetState.Downloading ->
                BannerData(null,  "Загрузка RU правил маршрутизации...", AccentSoft, Accent)
            is RuleSetState.Error       ->
                BannerData(null,  "Ошибка RU правил: ${state.message}", Border, TextMuted)
            else -> BannerData(null, "", AccentSoft, TextMuted)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (state is RuleSetState.Downloading || state is RuleSetState.Checking) {
                CircularProgressIndicator(
                    color = Accent,
                    trackColor = Border,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(text = text, color = fg, fontSize = 12.sp)
        }
    }
}

private data class BannerData(
    val iconRes: Any?,
    val text: String,
    val bg: androidx.compose.ui.graphics.Color,
    val fg: androidx.compose.ui.graphics.Color,
)
