package com.example

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BcvSyncScheduler {
    private const val SYNC_WORK_NAME = "bcv_periodic_sync_work"

    fun scheduleBackgroundSync(context: Context) {
        Log.d("BcvSyncScheduler", "Scheduling periodic background rate sync")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Execute every 2 hours
        val syncRequest = PeriodicWorkRequestBuilder<BcvSyncWorker>(2, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        try {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE, // REPLACE to ensure constraints and periodicity are applied newly
                syncRequest
            )
        } catch (e: Exception) {
            Log.e("BcvSyncScheduler", "Failed to schedule periodic background sync work", e)
        }
    }

    fun cancelBackgroundSync(context: Context) {
        Log.d("BcvSyncScheduler", "Cancelling periodic background rate sync")
        try {
            WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
        } catch (e: Exception) {
            Log.e("BcvSyncScheduler", "Failed to cancel dynamic background sync work", e)
        }
    }
}
