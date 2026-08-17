package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JarvisMonochromeColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = OledBlack,
    primaryContainer = OffWhite,
    onPrimaryContainer = OledBlack,
    secondary = OffWhite,
    onSecondary = OledBlack,
    secondaryContainer = DarkGray,
    onSecondaryContainer = PureWhite,
    tertiary = SilverText,
    onTertiary = OledBlack,
    background = OledBlack,
    onBackground = PureWhite,
    surface = DarkCharcoal,
    onSurface = PureWhite,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = SilverText,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorderHighlight,
    error = ErrorRedAlert,
    onError = PureWhite,
    errorContainer = ErrorContainerDark,
    onErrorContainer = PureWhite
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = JarvisMonochromeColorScheme,
        typography = Typography,
        content = content
    )
}
