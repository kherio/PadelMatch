package com.kherio.padelmatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta del club: dorado + negro + verde pista como acento secundario
val Gold = Color(0xFFC9A227)
val GoldLight = Color(0xFFE8C55A)
val GoldDark = Color(0xFF8F7112)
val AlmostBlack = Color(0xFF141414)
val CourtGreen = Color(0xFF1E8E5A)

private val DarkColors = darkColorScheme(
    primary = GoldLight,
    onPrimary = Color(0xFF3D2E00),
    primaryContainer = GoldDark,
    onPrimaryContainer = Color(0xFFFFE8A3),
    secondary = Color(0xFF7BD9A5),
    onSecondary = Color(0xFF00391D),
    secondaryContainer = Color(0xFF00522B),
    onSecondaryContainer = Color(0xFFC6F0D8),
    // Transparente: deja ver la textura de fondo que dibuja MainActivity
    background = Color.Transparent,
    onBackground = Color(0xFFF2EFE6),
    surface = Color(0xFF1F1F1F),
    onSurface = Color(0xFFF2EFE6),
    surfaceVariant = Color(0xFF2C2A22),
    onSurfaceVariant = Color(0xFFD6CDB0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF9A9280)
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontSize = 16.sp)
)

@Composable
fun PadelMatchTheme(content: @Composable () -> Unit) {
    // La identidad del club (negro + dorado) es oscura: la app usa siempre el tema oscuro.
    MaterialTheme(colorScheme = DarkColors, typography = AppTypography, content = content)
}
