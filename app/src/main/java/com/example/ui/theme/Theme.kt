package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Binance Aesthetic Theme Scheme
private val LightBinanceColorScheme = lightColorScheme(
    primary = BinanceYellowMuted,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF6D8),
    onPrimaryContainer = Color(0xFF8C6200),
    secondary = BinanceLightCard,
    onSecondary = BinanceLightDarkText,
    secondaryContainer = BinanceLightCard,
    onSecondaryContainer = BinanceLightDarkText,
    background = BinanceLightBg,
    onBackground = BinanceLightDarkText,
    surface = BinanceLightSurface,
    onSurface = BinanceLightDarkText,
    surfaceVariant = BinanceLightCard,
    onSurfaceVariant = BinanceLightMuted,
    outline = BinanceLightBorder,
    error = BinanceRed,
    onError = Color.White
)

// Dark Ambient Binance Theme Scheme
private val DarkBinanceColorScheme = darkColorScheme(
    primary = BinanceYellow,
    onPrimary = BinanceBlack,
    primaryContainer = Color(0xFF2B2205),
    onPrimaryContainer = BinanceYellow,
    secondary = BinanceSurfaceSecondary,
    onSecondary = BinanceWhiteText,
    secondaryContainer = BinanceSurfaceSecondary,
    onSecondaryContainer = BinanceWhiteText,
    background = BinanceBlack,
    onBackground = BinanceWhiteText,
    surface = BinanceSurface,
    onSurface = BinanceWhiteText,
    surfaceVariant = Color(0xFF202630),
    onSurfaceVariant = BinanceMutedText,
    outline = BinanceBorder,
    error = BinanceRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkBinanceColorScheme
    } else {
        LightBinanceColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
