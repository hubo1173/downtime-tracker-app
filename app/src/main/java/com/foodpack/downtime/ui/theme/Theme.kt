package com.foodpack.downtime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PrimaryBlue = Color(0xFF1E3A5F)
private val AccentOrange = Color(0xFFE8622C)

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = AccentOrange,
    surface = Color(0xFFFAFAFA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FA6D6),
    secondary = AccentOrange
)

@Composable
fun DowntimeTrackerTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
