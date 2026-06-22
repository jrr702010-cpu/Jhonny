package com.example

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.BcvDatabase
import com.example.data.BcvRateRecord
import com.example.data.BcvRepository
import com.example.network.BcvScraper
import com.example.network.GeminiNetwork
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BcvSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "BcvSyncWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "Background sync worker started execution")

        val context = applicationContext
        val sharedPrefs = context.getSharedPreferences("com.example.bcv_prefs", Context.MODE_PRIVATE)
        val isAutoRefreshEnabled = sharedPrefs.getBoolean("pref_auto_refresh_enabled", false)

        if (!isAutoRefreshEnabled) {
            Log.d(TAG, "Automatic background refresh is disabled in settings. Skipping work.")
            return Result.success()
        }

        try {
            val db = Room.databaseBuilder(
                context,
                BcvDatabase::class.java,
                "bcv_rates_v2.db"
            ).fallbackToDestructiveMigration().build()

            val dao = db.bcvDao()
            val repository = BcvRepository(dao)

            val oldRecord = repository.getLatestRate()

            // Fetch new rates
            Log.d(TAG, "Fetching rates directly...")
            var scrapResult = BcvScraper.fetchRatesDirectly()
            var sourceName = "BCV Oficial"
            var success = scrapResult.success

            if (!success) {
                Log.w(TAG, "Direct fetch failed: ${scrapResult.errorMsg}. Trying Gemini AI fallback...")
                scrapResult = GeminiNetwork.fetchRatesThroughAi()
                sourceName = "Asistente IA"
                success = scrapResult.success
            }

            if (success) {
                val nextRecord = BcvRateRecord(
                    dateText = scrapResult.dateText.ifEmpty {
                        SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "VE")).format(Date())
                    },
                    usd = scrapResult.usd,
                    eur = scrapResult.eur,
                    cny = scrapResult.cny,
                    tryLira = scrapResult.tryLira,
                    rub = scrapResult.rub,
                    source = "$sourceName (Fondo)"
                )

                repository.insertRate(nextRecord)
                Log.d(TAG, "Successfully saved background fetched rate usd=${nextRecord.usd}")

                // Notify UI and widget
                try {
                    BcvAppWidgetProvider.triggerUpdate(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating app widget", e)
                }

                // Check and trigger rate change notifications
                val notifEnabled = sharedPrefs.getBoolean("pref_notif_enabled", false)
                val notifyOnChanges = sharedPrefs.getBoolean("pref_notif_changes", false)

                if (notifEnabled && notifyOnChanges && oldRecord != null && oldRecord.usd != nextRecord.usd) {
                    try {
                        NotificationHelper.showRateChangeNotification(
                            context = context,
                            oldRate = oldRecord.usd,
                            newRate = nextRecord.usd,
                            dateStr = nextRecord.dateText
                        )
                        Log.d(TAG, "Sent rate modification push notification")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error firing rate notification", e)
                    }
                }
            } else {
                Log.e(TAG, "Background rate synchronization failed because both direct scraper and Gemini fallback failed")
            }

            db.close()
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Exception encountered during background sync execution", e)
            return Result.retry()
        }
    }
}
