package dev.mpa.client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MpaColors = darkColorScheme(
    primary = Go,
    onPrimary = PaperInk,
    primaryContainer = GoDeep,
    onPrimaryContainer = OnBoard,
    secondary = Signal,
    onSecondary = PaperInk,
    secondaryContainer = BoardRaised,
    onSecondaryContainer = OnBoard,
    tertiary = Signal,
    background = Ground,
    onBackground = OnBoard,
    surface = Ground,
    onSurface = OnBoard,
    surfaceVariant = BoardRaised,
    onSurfaceVariant = OnBoardMuted,
    surfaceContainerLowest = Ground,
    surfaceContainerLow = Board,
    surfaceContainer = Board,
    surfaceContainerHigh = BoardRaised,
    surfaceContainerHighest = BoardRaised,
    inverseSurface = Paper,
    inverseOnSurface = PaperInk,
    outline = Rule,
    outlineVariant = Rule,
    error = Fail,
    onError = PaperInk,
    errorContainer = FailDeep,
    onErrorContainer = OnBoard,
)

private val MpaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MpaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MpaColors,
        typography = MpaTypography,
        shapes = MpaShapes,
        content = content,
    )
}
