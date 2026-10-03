package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FuelStationColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = SurfaceWhite,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = PrimaryDark,
    secondary = BlueAccent,
    onSecondary = SurfaceWhite,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    background = BackgroundLight,
    onBackground = DarkSlate,
    surface = SurfaceWhite,
    onSurface = DarkSlate,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    outline = BorderColor,
    error = ErrorRed,
    onError = SurfaceWhite
)

@Composable
fun FuelStationTheme(
    darkTheme: Boolean = false, // Enforce crisp official Fuel Station identity palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FuelStationColorScheme,
        typography = Typography,
        content = content
    )
}
