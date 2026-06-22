package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bcv_rates")
data class BcvRateRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateText: String, // e.g. "Viernes, 21 de Junio, 2026"
    val usd: Double,      // Dólar
    val eur: Double,      // Euro
    val cny: Double,      // Yuan
    val tryLira: Double,  // Lira Turca
    val rub: Double,      // Rublo Ruso
    val source: String   // e.g. "BCV Oficial", "Asistente IA", "Simulado"
)
