package com.kherio.padelmatch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PadelGreen = Color(0xFFC9A227) // dorado, a juego con el logo del club
private val PadelLime = Color(0xFF1A1A1A) // negro del escudo

private val LightColors = lightColorScheme(
    primary = PadelGreen,
    secondary = PadelLime,
    surface = Color(0xFFFAFAFA)
)

private val DarkColors = darkColorScheme(
    primary = PadelLime,
    secondary = PadelGreen
)

@Composable
fun PadelMatchTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
