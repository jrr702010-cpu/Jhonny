package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BcvRateRecord::class], version = 1, exportSchema = false)
abstract class BcvDatabase : RoomDatabase() {
    abstract fun bcvDao(): BcvDao
}
