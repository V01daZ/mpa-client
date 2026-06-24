package dev.mpa.client.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.mpa.client.R

// ── Шрифты ────────────────────────────────────────────────────────────────────
//
// Нужно вручную добавить TTF-файлы в res/font/ (см. README).
// Если файлы не добавлены — сборка упадёт с "resource not found".
// При желании временно закомментировать FontFamily-блоки и использовать
// FontFamily.SansSerif / FontFamily.Monospace как fallback.

// Space Grotesk — display / заголовки (как --font-display на десктопе)
val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

// JetBrains Mono — моноширинный (как --font-mono)
val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
)

val MpaTypography = Typography(
    // Заголовок "MPA" в хедере
    headlineMedium = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 0.05.sp,
    ),
    // Текст на кнопке вкл/выкл
    labelSmall = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 0.2.sp,
    ),
    // Имя сервера в карточке
    bodyMedium = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    // Адрес сервера / пинг (моно)
    bodySmall = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
)
