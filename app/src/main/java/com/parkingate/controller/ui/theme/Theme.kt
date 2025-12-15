package com.parkingate.controller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = White,
    primaryContainer = OrangeLight,
    onPrimaryContainer = OrangeDark,
    secondary = Orange,
    onSecondary = White,
    background = White,
    onBackground = DarkGray,
    surface = LightGray,
    onSurface = DarkGray,
    surfaceVariant = Gray,
    onSurfaceVariant = DarkGray,
    error = Error,
    onError = White,
    outline = Gray
)

private val DarkColorScheme = darkColorScheme(
    primary = OrangeLight,
    onPrimary = Dark900,
    primaryContainer = OrangeDark,
    onPrimaryContainer = DarkTextPrimary,
    secondary = OrangeLight,
    onSecondary = Dark900,
    background = Dark900,
    onBackground = DarkTextPrimary,
    surface = Dark800,
    onSurface = DarkTextPrimary,
    surfaceVariant = Dark700,
    onSurfaceVariant = DarkTextSecondary,
    error = ErrorDark,
    onError = Dark900,
    outline = Dark600
)

@Composable
fun BluetoothDevAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
