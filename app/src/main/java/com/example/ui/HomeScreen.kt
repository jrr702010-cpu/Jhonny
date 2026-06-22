package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
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

    val converterInput by viewModel.converterInput.collectAsState()
    val converterOutput by viewModel.converterOutput.collectAsState()
    val isConvertingUsdToBs by viewModel.isConvertingUsdToBs.collectAsState()

    val rateValue = currentRate?.usd ?: 36.42
    val dateTextValue = currentRate?.dateText ?: "Cargando..."
    val sourceValue = currentRate?.source ?: "Cargando..."

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Floating Info Alerts ---
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

        // --- Main Hero Card: BCV official rate display ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MossSoft)
                .padding(24.dp)
                .testTag("rate_hero_card"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "TASA OFICIAL BCV",
                    color = MossGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val formattedRate = String.format("%.2f", rateValue).replace(".", ",")
                    Text(
                        text = formattedRate,
                        color = MossDark,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.5).sp,
                        lineHeight = 54.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bs/$",
                        color = MossDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Actualizado: $dateTextValue",
                    color = MossGreen.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center
                )
            }

            // Sync loading feedback
            if (isRefreshing) {
                CircularProgressIndicator(
                    color = MossGreen,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                )
            }
        }

        // --- Secondary Statistics Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Variation Card
            Card(
                colors = CardDefaults.cardColors(containerColor = ClayGrey),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .testTag("variation_card_button")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Variación",
                        color = MutedOlive,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val iconColor = if (isVariationPositive) ForestGreen else EarthRed
                        Text(
                            text = if (isVariationPositive) "▲" else "▼",
                            color = iconColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = variationPercent,
                            color = iconColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Source/Origin Card
            Card(
                colors = CardDefaults.cardColors(containerColor = ClayGrey),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .testTag("source_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Origen",
                        color = MutedOlive,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = sourceValue,
                        color = NaturalCharcoal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- Live Conversion Calculator Widget ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calculator_section")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CALCULADORA DE CONVERSIÓN",
                    color = MutedOlive,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Input Component
                OutlinedTextField(
                    value = converterInput,
                    onValueChange = { viewModel.onConverterInputChange(it) },
                    label = {
                        Text(
                            text = if (isConvertingUsdToBs) "Cantidad en Dólares ($)" else "Cantidad en Bolivars (Bs)",
                            color = MutedOlive,
                            fontSize = 13.sp
                        )
                    },
                    placeholder = { Text("0,00", color = MutedOlive.copy(alpha = 0.5f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MossGreen,
                        unfocusedBorderColor = StoneBeige,
                        cursorColor = MossGreen
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Dynamic Interchanging Button Row
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalDivider(color = StoneBeige, modifier = Modifier.fillMaxWidth())

                    IconButton(
                        onClick = { viewModel.toggleConversionDirection() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(SageGreen)
                            .testTag("calc_swap_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Cambiar Dirección",
                            tint = SageDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Output Component
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ClayGrey)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isConvertingUsdToBs) "Resultado en Bolivars" else "Resultado en Dólares",
                        color = MutedOlive,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$converterOutput ${if (isConvertingUsdToBs) "Bs" else "$"}",
                        color = MossDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("calc_output_text")
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
