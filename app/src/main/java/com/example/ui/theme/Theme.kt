package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Natural Tones Theme Scheme
private val LightNaturalColorScheme = lightColorScheme(
    primary = MossGreen,
    onPrimary = Color.White,
    primaryContainer = MossSoft,
    onPrimaryContainer = MossDark,
    secondary = SageGreen,
    onSecondary = SageDark,
    secondaryContainer = SageGreen,
    onSecondaryContainer = SageDark,
    background = NaturalLinen,
    onBackground = NaturalCharcoal,
    surface = Color.White,
    onSurface = NaturalCharcoal,
    surfaceVariant = ClayGrey,
    onSurfaceVariant = MutedOlive,
    outline = StoneBeige,
    error = EarthRed,
    onError = Color.White
)

// Dark Forestadapted Scheme for Dark Theme users
private val DarkNaturalColorScheme = darkColorScheme(
    primary = SageGreen,
    onPrimary = SageDark,
    primaryContainer = Color(0xFF2E3514),
    onPrimaryContainer = MossSoft,
    secondary = MossSoft,
    onSecondary = MossDark,
    secondaryContainer = Color(0xFF242C1D),
    onSecondaryContainer = SageGreen,
    background = Color(0xFF11120E),
    onBackground = Color(0xFFE6E2D8),
    surface = Color(0xFF1A1C16),
    onSurface = Color(0xFFE6E2D8),
    surfaceVariant = Color(0xFF23251F),
    onSurfaceVariant = MutedOlive,
    outline = Color(0xFF47493F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkNaturalColorScheme
    } else {
        LightNaturalColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
