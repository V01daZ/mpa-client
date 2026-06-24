package dev.mpa.client.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import dev.mpa.client.ui.theme.Accent
import dev.mpa.client.ui.theme.Connected
import kotlin.math.sin

/**
 * Анимированный фон — два блюрных пятна (медь/тил).
 * Зеркало src/components/AnimatedBackground.tsx.
 *
 * На Android blur реализуется через радиальный градиент с большим радиусом
 * (RenderScript-blur недоступен без импорта, Canvas-подход достаточен).
 */
@Composable
fun AnimatedBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bg")

    // Blob 1 — 22s цикл (Accent/медь)
    val t1 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1"
    )

    // Blob 2 — 26s цикл (Connected/тил)
    val t2 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Blob 1: от (-4rem, -4rem) → (+15%, +10%) — float-1
        val x1 = lerp(-0.15f * w, 0.15f * w, t1) + 0f
        val y1 = lerp(-0.1f * h, 0.1f * h, t1) + 0f
        val r1 = w * 0.45f * lerp(1f, 1.15f, t1)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Accent.copy(alpha = 0.25f), Accent.copy(alpha = 0f)),
                center = Offset(x1, y1),
                radius = r1,
            ),
            radius = r1,
            center = Offset(x1, y1),
        )

        // Blob 2: от (+10%, +15%) → (-15%, -10%) — float-2
        val x2 = lerp(w + 0.1f * w, w - 0.15f * w, t2)
        val y2 = lerp(h + 0.05f * h, h - 0.1f * h, t2)
        val r2 = w * 0.5f * lerp(1.1f, 0.95f, t2)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Connected.copy(alpha = 0.20f), Connected.copy(alpha = 0f)),
                center = Offset(x2, y2),
                radius = r2,
            ),
            radius = r2,
            center = Offset(x2, y2),
        )
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
