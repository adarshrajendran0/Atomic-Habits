package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = Gray80,
    onPrimaryContainer = White,
    secondary = Gray30,
    onSecondary = Black,
    tertiary = Gray50,
    onTertiary = White,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = Gray90,
    onSurfaceVariant = White,
    outline = Gray60,
    outlineVariant = Gray85
)

private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = Gray10,
    onPrimaryContainer = Black,
    secondary = Gray60,
    onSecondary = White,
    tertiary = Gray40,
    onTertiary = Black,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = Gray5,
    onSurfaceVariant = Black,
    outline = Gray40,
    outlineVariant = Gray20
)

// Grays used for extra styling outside material theme color tokens

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
