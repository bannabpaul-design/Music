package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MelodyDarkColorScheme = darkColorScheme(
    primary = PrimaryNeon,
    onPrimary = Color.White,
    primaryContainer = DarkSurfaceHighlight,
    onPrimaryContainer = PrimaryLight,
    secondary = SecondaryCyan,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = SecondaryCyan,
    tertiary = AccentPink,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF3E3259)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Music players are predominantly immersive dark themed for artwork focus
    MaterialTheme(
        colorScheme = MelodyDarkColorScheme,
        typography = Typography,
        content = content
    )
}
