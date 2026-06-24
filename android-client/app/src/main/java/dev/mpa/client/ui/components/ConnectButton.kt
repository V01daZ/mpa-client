package dev.mpa.client.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mpa.client.data.ConnectionStatus
import dev.mpa.client.ui.theme.Accent
import dev.mpa.client.ui.theme.Border
import dev.mpa.client.ui.theme.Connected
import dev.mpa.client.ui.theme.Error
import dev.mpa.client.ui.theme.Ink
import dev.mpa.client.ui.theme.SpaceGroteskFamily
import dev.mpa.client.ui.theme.Surface
import dev.mpa.client.ui.theme.SurfaceHover
import dev.mpa.client.ui.theme.TextMuted

/**
 * Круговая кнопка подключения.
 * Зеркало src/components/ConnectButton.tsx:
 * - SVG-кольцо → Canvas drawArc
 * - Анимированное кольцо при connecting/disconnecting
 * - Glow при connected
 */
@Composable
fun ConnectButton(
    status: ConnectionStatus,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 192.dp,
) {
    val isConnected = status is ConnectionStatus.Connected
    val isBusy = status.isBusy
    val isError = status is ConnectionStatus.Error

    val ringColor = when {
        isError -> Error
        isConnected -> Connected
        isBusy -> Accent
        else -> Border
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Glow-пульс для connected (аналог breathe-анимации)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Surface)
            .clickable(
                enabled = !isBusy,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .drawWithContent {
                drawContent()
                // Кольцо
                val strokeWidth = 4.dp.toPx()
                val inset = strokeWidth / 2
                val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
                val topLeft = Offset(inset, inset)

                if (isBusy) {
                    rotate(ringRotation) {
                        // Дашед-дуга (18% заполнено)
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = 360f * 0.18f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                } else {
                    val glowColor = if (isConnected) Connected.copy(alpha = glowAlpha) else Color.Transparent
                    if (isConnected) {
                        // Glow-кольцо под основным
                        drawArc(
                            color = glowColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(strokeWidth * 3, cap = StrokeCap.Round)
                        )
                    }
                    // Сплошное кольцо
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isBusy) {
                CircularProgressIndicator(
                    color = Accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Power,
                    contentDescription = if (isConnected) "Отключиться" else "Подключиться",
                    tint = when {
                        isConnected -> Connected
                        isError -> Error
                        else -> TextMuted
                    },
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                text = if (isConnected) "ВЫКЛ" else "ВКЛ",
                color = TextMuted,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.2.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
