package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.example.data.BcvDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class BcvReminderReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BcvReminderReceiver", "Alarm onReceive triggered")

        // First verify if daily reminders are enabled in shared preferences
        val sharedPrefs = context.getSharedPreferences("com.example.bcv_prefs", Context.MODE_PRIVATE)
        val enabled = sharedPrefs.getBoolean("pref_daily_reminder_enabled", false)
        if (!enabled) {
            Log.d("BcvReminderReceiver", "Daily reminders are disabled in settings, skipping notification")
            return
        }

        // Query rate in background thread
        receiverScope.launch {
            try {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    BcvDatabase::class.java,
                    "bcv_rates_v2.db"
                ).fallbackToDestructiveMigration().build()

                val latestRate = db.bcvDao().getLatestRate()
                db.close()

                if (latestRate != null) {
                    NotificationHelper.showDailyReminderNotification(
                        context = context,
                        rateUsd = latestRate.usd,
                        rateEur = latestRate.eur,
                        dateStr = latestRate.dateText
                    )
                } else {
                    Log.d("BcvReminderReceiver", "No rate record available to notify")
                }
            } catch (e: Exception) {
                Log.e("BcvReminderReceiver", "Error querying rate inside broadcast", e)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 3001

        fun scheduleReminder(context: Context, timeStr: String) {
            // Parse timeStr (e.g. "09:00", "13:00", "18:00")
            val parts = timeStr.split(":")
            if (parts.size != 2) return
            val hour = parts[0].toIntOrNull() ?: 9
            val minute = parts[1].toIntOrNull() ?: 0

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, BcvReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Setup Calendar
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If scheduled time has already passed for today, set it for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                // Exact alarm is not strictly necessary for simple price updates, setInexactRepeating is battery friendly:
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
                Log.d("BcvReminderReceiver", "Alarm scheduled daily at $hour:$minute. Next trigger: ${calendar.time}")
            } catch (e: Exception) {
                Log.e("BcvReminderReceiver", "Failed to schedule alarm", e)
            }
        }

        fun cancelReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, BcvReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d("BcvReminderReceiver", "Scheduled alarm cancelled")
            }
        }
    }
}
