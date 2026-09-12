package com.one.cognitivecompanion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OneCyanDark,
    onPrimary = OneInverseSurface,
    primaryContainer = Color(0xFF0B4B5C),
    onPrimaryContainer = OneCyanDark,
    secondary = OneMintDark,
    onSecondary = OneInverseSurface,
    tertiary = OneAmberDark,
    onTertiary = OneInverseSurface,
    background = OneCanvasDark,
    surface = OneSurfaceDark,
    onBackground = OneInkDark,
    onSurface = OneInkDark,
    onSurfaceVariant = OneSecondaryInkDark
)

private val LightColorScheme = lightColorScheme(
    primary = OneBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E6FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = OneCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC6F1F3),
    onSecondaryContainer = Color(0xFF002022),
    tertiary = OneMint,
    onTertiary = Color.White,
    background = OneCanvasLight,
    surface = Color.White,
    onBackground = OneInkLight,
    onSurface = OneInkLight,
    onSurfaceVariant = OneSecondaryInkLight
)

@Composable
fun ONETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // ONE uses a stable brand palette. Dynamic colour can be enabled later
    // for a platform-adapted variant without changing the app architecture.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Keep the branch explicit so the theme remains easy to test and to adapt
    // when the product eventually exposes a user-controlled appearance choice.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
