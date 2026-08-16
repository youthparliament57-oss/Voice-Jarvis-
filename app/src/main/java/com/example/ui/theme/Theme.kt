package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val JarvisDarkColorScheme =
  darkColorScheme(
    primary = CyanAccent, 
    secondary = BlueAccent, 
    tertiary = TealAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onTertiary = DarkBackground,
    onBackground = LightText,
    onSurface = LightText
  )

private val JarvisLightColorScheme =
  lightColorScheme(
    primary = CyanAccent,
    secondary = BlueAccent,
    tertiary = TealAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onTertiary = DarkBackground,
    onBackground = LightText,
    onSurface = LightText
  )

@Composable
fun JarvisTheme(
  darkTheme: Boolean = true, // Force dark theme for JARVIS
  dynamicColor: Boolean = false, // Disable dynamic colors to keep the tech look
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) JarvisDarkColorScheme else JarvisLightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
