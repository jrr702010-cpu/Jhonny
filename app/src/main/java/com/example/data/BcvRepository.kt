package com.example.data

import kotlinx.coroutines.flow.Flow

class BcvRepository(private val bcvDao: BcvDao) {
    val allRates: Flow<List<BcvRateRecord>> = bcvDao.getAllRatesFlow()

    suspend fun getLatestRate(): BcvRateRecord? {
        return bcvDao.getLatestRate()
    }

    suspend fun getPreviousRate(currentId: Int): BcvRateRecord? {
        return bcvDao.getPreviousRate(currentId)
    }

    suspend fun insertRate(rate: BcvRateRecord) {
        bcvDao.insertRate(rate)
    }

    suspend fun clearRates() {
        bcvDao.clearRates()
    }
}
