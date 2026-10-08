package com.foodpack.downtime.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shop-floor colors: dark background (screens are readable under bright
 * plant lighting and don't glare), traffic-light status colors, and a
 * single accent used for primary actions.
 */
private val AccentOrange = Color(0xFFE8622C)
private val SurfaceDark = Color(0xFF121417)
private val SurfaceCardDark = Color(0xFF1E2126)

object StatusColors {
    val Running = Color(0xFF2E7D32)   // green
    val Changeover = Color(0xFFF5A623) // amber
    val Stopped = Color(0xFFD64541)    // red
}

val LocalStatusColors = staticCompositionLocalOf { StatusColors }

private val DarkColors = darkColorScheme(
    primary = AccentOrange,
    onPrimary = Color(0xFF1A1200),
    secondary = StatusColors.Running,
    tertiary = StatusColors.Changeover,
    error = StatusColors.Stopped,
    background = SurfaceDark,
    surface = SurfaceCardDark,
    surfaceVariant = Color(0xFF2A2E35),
    secondaryContainer = Color(0xFF3A3024),
    onSecondaryContainer = AccentOrange
)

// Soft, modern rounding across cards, buttons and chips — gives the dark
// theme a friendlier, less utilitarian feel than sharp Material defaults.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Dark mode is the default for this app regardless of the phone's system
 * setting — the shop floor is bright and a dark UI reads better there,
 * and it keeps the traffic-light status colors consistent for everyone.
 */
@Composable
fun DowntimeTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, shapes = AppShapes, content = content)
}
