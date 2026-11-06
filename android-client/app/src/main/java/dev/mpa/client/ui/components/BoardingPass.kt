package dev.mpa.client.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mpa.client.data.ConnectionStatus
import dev.mpa.client.data.ServerProfile
import dev.mpa.client.ui.theme.FailDeep
import dev.mpa.client.ui.theme.Go
import dev.mpa.client.ui.theme.GoDeep
import dev.mpa.client.ui.theme.Mono
import dev.mpa.client.ui.theme.Paper
import dev.mpa.client.ui.theme.PaperInk
import dev.mpa.client.ui.theme.PaperMuted
import dev.mpa.client.ui.theme.PaperRule
import dev.mpa.client.ui.theme.Signal
import kotlinx.coroutines.delay

/**
 * Талон соединения: откуда → куда → пинг, линия отрыва, статус и время в пути,
 * и одна главная кнопка. Заменяет круглую кнопку питания.
 */
@Composable
fun BoardingPass(
    status: ConnectionStatus,
    profile: ServerProfile?,
    ping: Int?,
    onToggle: () -> Unit,
    onAddServer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val notch = 10.dp
    Column(modifier) {
        // ── Верхний сегмент: маршрут ───────────────────────────────────────
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(TicketShape(notch, notchAtTop = false))
                .background(Paper)
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp)
        ) {
            Segment(label = "ОТКУДА") {
                Text("Ты", color = PaperInk, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = PaperMuted,
                modifier = Modifier.padding(top = 26.dp).size(18.dp),
            )
            Segment(label = "КУДА", modifier = Modifier.weight(1f)) {
                Text(
                    text = profile?.name ?: "—",
                    color = PaperInk,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Segment(label = "ПИНГ", horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    BigValue(
                        when {
                            profile == null -> "—"
                            ping == null -> "…"
                            ping < 0 -> "×"
                            else -> ping.toString()
                        },
                        color = if (ping != null && ping < 0) FailDeep else PaperInk,
                    )
                    if (ping != null && ping >= 0) {
                        Text("мс", color = PaperMuted, fontFamily = Mono, fontSize = 13.sp,
                            modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
                    }
                }
            }
        }

        // ── Нижний сегмент: статус и действие ──────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TicketShape(notch, notchAtTop = true))
                .background(Paper)
                .drawBehind { perforation(notch.toPx()) }
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                Segment(label = "СТАТУС", modifier = Modifier.weight(1f)) {
                    StatusValue(status)
                }
                Segment(label = "В ПУТИ", horizontalAlignment = Alignment.End) {
                    BigValue(elapsedLabel(status), size = 20)
                }
            }

            if (status is ConnectionStatus.Error) {
                Text(
                    text = status.message,
                    color = FailDeep,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            Spacer(Modifier.height(18.dp))
            PassButton(status = status, hasProfile = profile != null, onToggle = onToggle, onAddServer = onAddServer)
        }
    }
}

@Composable
private fun Segment(
    label: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(label, color = PaperMuted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun BigValue(text: String, color: Color = PaperInk, size: Int = 24) {
    Text(text, color = color, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = size.sp, maxLines = 1)
}

@Composable
private fun StatusValue(status: ConnectionStatus) {
    val (label, dot) = when (status) {
        is ConnectionStatus.Connected -> "Подключено" to GoDeep
        is ConnectionStatus.Connecting -> "Подключение…" to Signal
        is ConnectionStatus.Disconnecting -> "Отключение…" to Signal
        is ConnectionStatus.Error -> "Ошибка" to FailDeep
        ConnectionStatus.Disconnected -> "Не защищено" to PaperMuted
    }
    val dotColor by animateColorAsState(dot, label = "statusDot")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(8.dp))
        Text(label, color = PaperInk, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun elapsedLabel(status: ConnectionStatus): String {
    if (status !is ConnectionStatus.Connected) return "—"
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(status.startedAt) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }
    val s = ((now - status.startedAt) / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60)
}

@Composable
private fun PassButton(
    status: ConnectionStatus,
    hasProfile: Boolean,
    onToggle: () -> Unit,
    onAddServer: () -> Unit,
) {
    val connected = status is ConnectionStatus.Connected
    val busy = status.isBusy
    val label = when {
        !hasProfile -> "Добавить сервер"
        status is ConnectionStatus.Connecting -> "Отменить"
        status is ConnectionStatus.Disconnecting -> "Отключение…"
        connected -> "Отключить"
        status is ConnectionStatus.Error -> "Повторить"
        else -> "Подключить"
    }
    // Зелёный в интерфейсе означает только «подключено»: зелёная кнопка — лишь «Отключить».
    // Во всех остальных состояниях кнопка тёмная, чтобы «Не защищено» не выглядело как «всё хорошо».
    val container = if (connected) Go else PaperInk
    val content = if (connected) PaperInk else Paper
    Button(
        onClick = if (hasProfile) onToggle else onAddServer,
        enabled = status !is ConnectionStatus.Disconnecting,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = PaperRule,
            disabledContentColor = PaperMuted,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(
                color = Signal,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Линия отрыва по верхнему краю нижнего сегмента. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.perforation(notchPx: Float) {
    val y = 0.5.dp.toPx()
    drawLine(
        color = PaperRule,
        start = Offset(notchPx + 6.dp.toPx(), y),
        end = Offset(size.width - notchPx - 6.dp.toPx(), y),
        strokeWidth = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
    )
}

/** Прямоугольник со скруглёнными углами и двумя полукруглыми вырезами на линии отрыва. */
private class TicketShape(private val notch: androidx.compose.ui.unit.Dp, private val notchAtTop: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { notch.toPx() }
        val corner = with(density) { 18.dp.toPx() }
        val body = Path().apply {
            val cr = CornerRadius(corner)
            val flat = CornerRadius.Zero
            addRoundRect(
                RoundRect(
                    left = 0f, top = 0f, right = size.width, bottom = size.height,
                    topLeftCornerRadius = if (notchAtTop) flat else cr,
                    topRightCornerRadius = if (notchAtTop) flat else cr,
                    bottomLeftCornerRadius = if (notchAtTop) cr else flat,
                    bottomRightCornerRadius = if (notchAtTop) cr else flat,
                )
            )
        }
        val y = if (notchAtTop) 0f else size.height
        val holes = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(Offset(0f, y), r))
            addOval(androidx.compose.ui.geometry.Rect(Offset(size.width, y), r))
        }
        return Outline.Generic(Path().apply { op(body, holes, PathOperation.Difference) })
    }
}
