package dev.mpa.client.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mpa.client.data.ServerProfile
import dev.mpa.client.ui.theme.Board
import dev.mpa.client.ui.theme.BoardRaised
import dev.mpa.client.ui.theme.Fail
import dev.mpa.client.ui.theme.Go
import dev.mpa.client.ui.theme.MonoData
import dev.mpa.client.ui.theme.OnBoard
import dev.mpa.client.ui.theme.OnBoardMuted
import dev.mpa.client.ui.theme.Paper
import dev.mpa.client.ui.theme.PaperInk
import dev.mpa.client.ui.theme.Rule
import dev.mpa.client.ui.theme.Signal
import dev.mpa.client.ui.theme.SignalWash

/** Шапка табло: колонки «Сервер / Пинг». */
@Composable
fun BoardHeader(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        // end = 56dp: колонка «ПИНГ» стоит над значениями, левее кнопки меню строки
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 56.dp)) {
            Text("СЕРВЕР", color = OnBoardMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("ПИНГ", color = OnBoardMuted, style = MaterialTheme.typography.labelSmall)
        }
        HorizontalDivider(color = Rule, modifier = Modifier.padding(top = 10.dp))
    }
}

/** Последняя строка табло: добавить ещё сервер. */
@Composable
fun AddServerRow(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onAdd,
        colors = ButtonDefaults.textButtonColors(contentColor = OnBoard),
        modifier = modifier.padding(horizontal = 8.dp).defaultMinSize(minHeight = 48.dp),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Добавить сервер", style = MaterialTheme.typography.labelMedium)
    }
}

/** Заголовок группы-подписки: название, число серверов, обновить, меню, свернуть. */
@Composable
fun GroupHeader(
    title: String,
    count: Int,
    collapsed: Boolean,
    refreshing: Boolean,
    onToggle: () -> Unit,
    onRefresh: () -> Unit,
    onRename: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = if (collapsed) "Развернуть" else "Свернуть", onClick = onToggle)
            .padding(start = 20.dp, end = 4.dp, top = 6.dp, bottom = 2.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = OnBoard, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(serversLabel(count), color = OnBoardMuted, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onRefresh, enabled = !refreshing) {
            if (refreshing) {
                CircularProgressIndicator(color = Signal, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить подписку", tint = OnBoardMuted)
            }
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Действия с подпиской", tint = OnBoardMuted)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Переименовать") }, onClick = { menu = false; onRename() })
            }
        }
        Icon(
            if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = null,
            tint = OnBoardMuted,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
}

/**
 * Строка табло. [changed] — пинг заметно изменился с прошлого взгляда:
 * строка держит коралловую подсветку, пока пользователь не взаимодействует с табло.
 */
@Composable
fun BoardRow(
    profile: ServerProfile,
    isActive: Boolean,
    isConnected: Boolean,
    ping: Int?,
    changed: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        when {
            changed -> SignalWash
            isActive -> BoardRaised
            else -> Color.Transparent
        },
        animationSpec = tween(400),
        label = "rowBg",
    )
    val pingText = when {
        ping == null -> "…"
        ping < 0 -> "нет связи"
        else -> "$ping мс"
    }
    val pingColor = when {
        ping == null -> OnBoardMuted
        ping < 0 -> Fail
        changed -> Signal
        else -> OnBoard
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .semantics {
                stateDescription = if (isActive) "Выбран" else "Не выбран"
                contentDescription = "${profile.name}, пинг $pingText"
            }
            .defaultMinSize(minHeight = 56.dp)
            .padding(start = 12.dp, end = 0.dp, top = 4.dp, bottom = 4.dp)
    ) {
        // Метка выбора: заполненный кружок — выбран (зелёный, если через него подключены)
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .then(
                    if (isActive) Modifier.background(if (isConnected) Go else Paper)
                    else Modifier.border(1.5.dp, Rule, CircleShape)
                )
        )
        Spacer(Modifier.width(14.dp))
        Text(
            profile.name,
            color = OnBoard,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(pingText, color = pingColor, style = MonoData, modifier = Modifier.padding(start = 12.dp))
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Действия с сервером", tint = OnBoardMuted)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                // Адрес — для владельца, не для каждой строки табло
                DropdownMenuItem(
                    text = { Text("${profile.address}:${profile.port}", style = MonoData, color = OnBoardMuted) },
                    onClick = {},
                    enabled = false,
                )
                DropdownMenuItem(
                    text = { Text("Удалить", color = Fail) },
                    onClick = { menu = false; onRemove() },
                )
            }
        }
    }
}

@Composable
fun BoardDivider() = HorizontalDivider(color = Rule, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))

/** Пустое табло: первый запуск. */
@Composable
fun EmptyBoard(onAdd: () -> Unit, onScan: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.large)
            .background(Board)
            .padding(20.dp)
    ) {
        Text("Табло пустое", color = OnBoard, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Вставь ключ от бота, ссылку vless:// или ссылку на подписку. Можно просто отсканировать QR-код.",
            color = OnBoardMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        // Кнопки друг под другом: при крупном системном шрифте подписи не обрезаются
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = Paper, contentColor = PaperInk),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            ) { Text("Вставить ключ") }
            OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = OnBoard, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("QR-код", color = OnBoard)
            }
        }
    }
}

/** «1 сервер», «3 сервера», «5 серверов». */
fun serversLabel(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    val word = when {
        mod10 == 1 && mod100 != 11 -> "сервер"
        mod10 in 2..4 && mod100 !in 12..14 -> "сервера"
        else -> "серверов"
    }
    return "$n $word"
}
