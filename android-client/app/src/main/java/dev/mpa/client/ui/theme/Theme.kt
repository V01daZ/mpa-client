package dev.mpa.client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MpaDarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Ink,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Accent,
    secondary = Connected,
    onSecondary = Ink,
    secondaryContainer = ConnectedSoft,
    onSecondaryContainer = Connected,
    tertiary = Accent,
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHover,
    onSurfaceVariant = TextMuted,
    outline = Border,
    error = Error,
    onError = Ink,
    errorContainer = ErrorSoft,
    onErrorContainer = Error,
)

@Composable
fun MpaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MpaDarkColorScheme,
        typography = MpaTypography,
        content = content,
    )
}
