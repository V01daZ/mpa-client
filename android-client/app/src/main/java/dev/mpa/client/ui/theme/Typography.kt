package dev.mpa.client.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.mpa.client.R

// Вариативные шрифты из res/font (OFL). Один файл — все начертания.

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variable(res: Int, weight: FontWeight) = Font(
    resId = res,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Golos Text — слова: заголовки, подписи, кнопки. Хорошая кириллица. */
val Golos = FontFamily(
    variable(R.font.golos_text, FontWeight.Normal),
    variable(R.font.golos_text, FontWeight.Medium),
    variable(R.font.golos_text, FontWeight.SemiBold),
    variable(R.font.golos_text, FontWeight.Bold),
)

/** JetBrains Mono — только числа и коды: пинг, адреса, таймер. */
val Mono = FontFamily(
    variable(R.font.jetbrains_mono, FontWeight.Normal),
    variable(R.font.jetbrains_mono, FontWeight.Medium),
    variable(R.font.jetbrains_mono, FontWeight.Bold),
)

private val base = Typography()

val MpaTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = Mono, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = Golos, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = Golos, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = Golos, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = Golos, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Golos),
    bodyMedium = base.bodyMedium.copy(fontFamily = Golos),
    bodySmall = base.bodySmall.copy(fontFamily = Golos),
    labelLarge = base.labelLarge.copy(fontFamily = Golos, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelMedium = base.labelMedium.copy(fontFamily = Golos, fontWeight = FontWeight.Medium),
    // Подписи сегментов талона: ОТКУДА / КУДА / ПИНГ
    labelSmall = TextStyle(
        fontFamily = Golos,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.9.sp,
    ),
)

/** Табличные цифры для пинга/адресов в строках табло. */
val MonoData = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp)
