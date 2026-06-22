package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentRate by viewModel.currentRate.collectAsState()
    val ratesHistory by viewModel.ratesHistory.collectAsState()

    val bcvRate = currentRate ?: BcvRateTemplateEmpty

    // Setup currency values mapped array
    val currencies = listOf(
        CurrencyRateItem("USD", "Dólar estadounidense", "🇺🇸", bcvRate.usd),
        CurrencyRateItem("EUR", "Euro europeo", "🇪🇺", bcvRate.eur),
        CurrencyRateItem("CNY", "Yuan chino", "🇨🇳", bcvRate.cny),
        CurrencyRateItem("TRY", "Lira turca", "🇹🇷", bcvRate.tryLira),
        CurrencyRateItem("RUB", "Rublo ruso", "🇷🇺", bcvRate.rub)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Multi-Currency Table ---
        Text(
            text = "DIVISAS OFICIALES BCV",
            color = MutedOlive,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // Draw currency grid columns
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            currencies.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ClayGrey)
                        .padding(12.dp)
                        .testTag("currency_row_${item.code}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = item.emoji, fontSize = 22.sp)
                        Column {
                            Text(
                                text = item.code,
                                color = NaturalCharcoal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.name,
                                color = MutedOlive,
                                fontSize = 11.sp
                            )
                        }
                    }

                    val formattedVal = String.format("%.4f", item.value).replace(".", ",")
                    Text(
                        text = "$formattedVal Bs",
                        color = MossDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- Recent History Tab Header ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "HISTÓRICO RECIENTE",
                color = MutedOlive,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Button to wipe logs
            if (ratesHistory.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearRates() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("clear_history_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Limpiar Historial",
                        tint = EarthRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- Recycler historical listing ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (ratesHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Historial vacío",
                            color = MutedOlive,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Presiona refrescar en inicio para sincronizar.",
                            color = MutedOlive.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("history_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ratesHistory) { record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = record.dateText,
                                    color = NaturalCharcoal,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Origen: ${record.source}",
                                    color = MutedOlive,
                                    fontSize = 11.sp
                                )
                            }

                            val usdValue = String.format("%.2f", record.usd).replace(".", ",")
                            Text(
                                text = "$usdValue Bs",
                                color = MossGreen,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class CurrencyRateItem(
    val code: String,
    val name: String,
    val emoji: String,
    val value: Double
)

private val BcvRateTemplateEmpty = com.example.data.BcvRateRecord(
    dateText = "Sin datos",
    usd = 0.0,
    eur = 0.0,
    cny = 0.0,
    tryLira = 0.0,
    rub = 0.0,
    source = ""
)
