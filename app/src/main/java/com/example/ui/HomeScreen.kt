package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentRate by viewModel.currentRate.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    val variationPercent by viewModel.variationPercent.collectAsState()
    val isVariationPositive by viewModel.isVariationPositive.collectAsState()

    val themeMode by viewModel.themeMode.collectAsState()
    
    val particlesEnabled by viewModel.particlesEnabled.collectAsState()
    val particlesInteractive by viewModel.particlesInteractive.collectAsState()
    val particlesPalette by viewModel.particlesPalette.collectAsState()
    val particlesCount by viewModel.particlesCount.collectAsState()
    val particlesSizeScale by viewModel.particlesSizeScale.collectAsState()
    val particlesSpeedScale by viewModel.particlesSpeedScale.collectAsState()

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val rateValue = currentRate?.usd ?: 36.42
    val dateTextValue = currentRate?.dateText ?: "Cargando..."
    val sourceValue = currentRate?.source ?: "BCV Oficial"

    var touchX by remember { mutableStateOf<Float?>(null) }
    var touchY by remember { mutableStateOf<Float?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(particlesInteractive) {
                if (particlesInteractive) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val anyPressed = event.changes.any { it.pressed }
                            if (anyPressed) {
                                val activePointer = event.changes.firstOrNull { it.pressed }
                                if (activePointer != null) {
                                    touchX = activePointer.position.x
                                    touchY = activePointer.position.y
                                }
                            } else {
                                touchX = null
                                touchY = null
                            }
                        }
                    }
                }
            }
    ) {
        // --- High Fidelity Interactive Node Connection background particle simulation ---
        InteractiveParticleBackground(
            enabled = particlesEnabled,
            interactive = particlesInteractive,
            palette = particlesPalette,
            isDarkTheme = isDarkTheme,
            count = particlesCount,
            sizeScale = particlesSizeScale,
            speedScale = particlesSpeedScale,
            touchX = touchX,
            touchY = touchY
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Error Alert Banner ---
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("error_alert")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // --- Live Price / Hero Rate Display ---
            // Styled as a high contrast dynamic glass card with dynamic glow outlines matching Chosen Particle Palette!
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) {
                        Color(0xFF181A20).copy(alpha = 0.85f)
                    } else {
                        Color(0xFFFFFFFF).copy(alpha = 0.85f)
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isDarkTheme) {
                        particlesPalette.darkPrimary.copy(alpha = 0.28f)
                    } else {
                        particlesPalette.lightPrimary.copy(alpha = 0.28f)
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rate_hero_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Badge styled for crypto exchange feel
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "BCV OFICIAL",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val formattedRate = String.format("%.2f", rateValue).replace(".", ",")
                            Text(
                                text = formattedRate,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 54.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1).sp,
                                lineHeight = 54.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bs/$",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Actualizado: $dateTextValue",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = FontStyle.Normal,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Smooth sync feedback inside card top corner
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }

            // --- Market Stats Grid Cards (Binance styled) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Variation Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) {
                            Color(0xFF181A20).copy(alpha = 0.85f)
                        } else {
                            Color(0xFFFFFFFF).copy(alpha = 0.85f)
                        }
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .testTag("variation_card_button")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Variación 24h",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val colorText = if (isVariationPositive) BinanceGreen else BinanceRed
                            val prefix = if (isVariationPositive) "▲" else "▼"
                            Text(
                                text = prefix,
                                color = colorText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = variationPercent,
                                color = colorText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Source/Origin Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) {
                            Color(0xFF181A20).copy(alpha = 0.85f)
                        } else {
                            Color(0xFFFFFFFF).copy(alpha = 0.85f)
                        }
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .testTag("source_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Proveedor",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = sourceValue,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ThemeSelectorSection(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "TEMA VISUAL",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Light Mode Option
                ThemeOptionButton(
                    label = "Claro ☀️",
                    selected = currentTheme == ThemeMode.LIGHT,
                    onClick = { onThemeSelected(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f)
                )

                // Dark Mode Option
                ThemeOptionButton(
                    label = "Oscuro 🌙",
                    selected = currentTheme == ThemeMode.DARK,
                    onClick = { onThemeSelected(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f)
                )

                // System Auto Option
                ThemeOptionButton(
                    label = "Auto ⚙️",
                    selected = currentTheme == ThemeMode.SYSTEM,
                    onClick = { onThemeSelected(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ThemeOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center
        )
    }
}
