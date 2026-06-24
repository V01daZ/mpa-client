package dev.mpa.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mpa.client.data.ServerProfile
import dev.mpa.client.ui.theme.Accent
import dev.mpa.client.ui.theme.AccentSoft
import dev.mpa.client.ui.theme.Border
import dev.mpa.client.ui.theme.Error
import dev.mpa.client.ui.theme.ErrorSoft
import dev.mpa.client.ui.theme.JetBrainsMonoFamily
import dev.mpa.client.ui.theme.SpaceGroteskFamily
import dev.mpa.client.ui.theme.Surface
import dev.mpa.client.ui.theme.TextMuted
import dev.mpa.client.ui.theme.TextPrimary

/**
 * Карточка одного сервера в нижней секции.
 * Зеркало src/components/ServerCard.tsx.
 */
@Composable
fun ServerCard(
    profile: ServerProfile,
    isActive: Boolean,
    ping: Int?,          // null = ещё не измерено, -1 = недоступен
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pingLabel = when (ping) {
        null -> "..."
        -1 -> "—"
        else -> "${ping} мс"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) AccentSoft else Surface)
            .border(
                width = 1.dp,
                color = if (isActive) Accent else Border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Индикатор активности (точка)
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isActive) Accent else Border)
        )

        // Имя + адрес
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.name,
                color = TextPrimary,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${profile.address}:${profile.port}",
                color = TextMuted,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Пинг
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                Icons.Default.Wifi,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = pingLabel,
                color = TextMuted,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
            )
        }

        // Удалить
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Удалить сервер",
                tint = Error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
