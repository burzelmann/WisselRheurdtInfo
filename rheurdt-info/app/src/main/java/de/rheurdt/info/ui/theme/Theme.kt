package de.rheurdt.info.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EmeraldPrimary = Color(0xFF10B981)
val EmeraldDark = Color(0xFF064E3B)
val AmberAccent = Color(0xFFF59E0B)
val DarkBackground = Color(0xFF0D1410)
val DarkSurface = Color(0xFF15221B)
val DarkSurfaceVariant = Color(0xFF1F3329)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = AmberAccent,
    onSecondary = Color.Black,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurface = Color(0xFFF1F5F3)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = EmeraldDark,
    secondary = AmberAccent,
    background = Color(0xFFF4F7F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFE5EDE8)
)

@Composable
fun RheurdtTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
