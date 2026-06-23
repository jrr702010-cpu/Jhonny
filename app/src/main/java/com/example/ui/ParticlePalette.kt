package com.example.ui

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class ParticlePalette(
    val id: String,
    val displayName: String,
    val lightPrimary: Color,
    val lightSecondary: Color,
    val darkPrimary: Color,
    val darkSecondary: Color
) {
    DEFAULT(
        "DEFAULT",
        "Clásico Oro",
        BinanceYellowMuted,
        BinanceYellow.copy(alpha = 0.5f),
        BinanceYellow,
        BinanceYellowMuted.copy(alpha = 0.5f)
    ),
    OCEAN(
        "OCEAN",
        "Azul Éter",
        Color(0xFF0077B6),
        Color(0xFF00B4D8).copy(alpha = 0.5f),
        Color(0xFF90E0EF),
        Color(0xFF0077B6).copy(alpha = 0.5f)
    ),
    EMERALD(
        "EMERALD",
        "Menta Verde",
        Color(0xFF0B9B60),
        BinanceGreen.copy(alpha = 0.5f),
        BinanceGreen,
        Color(0xFF039356).copy(alpha = 0.5f)
    ),
    PURPLE(
        "PURPLE",
        "Lavanda Neón",
        Color(0xFF6C5CE7),
        Color(0xFFa29bfe).copy(alpha = 0.5f),
        Color(0xFFBB86FC),
        Color(0xFF6C5CE7).copy(alpha = 0.5f)
    ),
    SUNSET(
        "SUNSET",
        "Atardecer",
        Color(0xFFDC5A00),
        Color(0xFFFF7675).copy(alpha = 0.5f),
        Color(0xFFFF7675),
        Color(0xFFE15F41).copy(alpha = 0.5f)
    )
}
