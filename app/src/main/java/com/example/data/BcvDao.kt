package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BcvDao {
    @Query("SELECT * FROM bcv_rates ORDER BY timestamp DESC")
    fun getAllRatesFlow(): Flow<List<BcvRateRecord>>

    @Query("SELECT * FROM bcv_rates ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRate(): BcvRateRecord?

    @Query("SELECT * FROM bcv_rates WHERE id < :currentId ORDER BY id DESC LIMIT 1")
    suspend fun getPreviousRate(currentId: Int): BcvRateRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: BcvRateRecord)

    @Query("DELETE FROM bcv_rates")
    suspend fun clearRates()
}
