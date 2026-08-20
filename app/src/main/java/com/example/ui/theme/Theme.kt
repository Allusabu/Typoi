package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SophisticatedDarkColorScheme =
  darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    primaryContainer = SophisticatedPrimaryContainer,
    onPrimaryContainer = SophisticatedOnPrimaryContainer,
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    tertiary = SophisticatedTertiary,
    background = SophisticatedDarkBackground,
    onBackground = SophisticatedTextPrimary,
    surface = SophisticatedDarkSurface,
    onSurface = SophisticatedTextPrimary,
    surfaceVariant = SophisticatedDarkSurfaceVariant,
    onSurfaceVariant = SophisticatedTextSecondary,
    outline = SophisticatedDarkOutline
  )

private val SophisticatedLightColorScheme =
  lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to Sophisticated Dark aesthetic
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) SophisticatedDarkColorScheme else SophisticatedLightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}


