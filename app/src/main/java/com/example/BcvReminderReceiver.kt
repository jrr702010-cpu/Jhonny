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
            // Legacy/fallback support
            scheduleCustomReminders(context, listOf(timeStr))
        }

        fun scheduleCustomReminders(context: Context, times: List<String>) {
            cancelAllReminders(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            times.forEachIndexed { index, timeStr ->
                val parts = timeStr.split(":")
                if (parts.size != 2) return@forEachIndexed
                val hour = parts[0].toIntOrNull() ?: 9
                val minute = parts[1].toIntOrNull() ?: 0

                val intent = Intent(context, BcvReminderReceiver::class.java)
                val requestCode = 3001 + index
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                try {
                    alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                    Log.d("BcvReminderReceiver", "Custom Alarm scheduled daily at $hour:$minute with code $requestCode. Next trigger: ${calendar.time}")
                } catch (e: Exception) {
                    Log.e("BcvReminderReceiver", "Failed to schedule custom alarm", e)
                }
            }
            // Save the number of scheduled alarms so we know how many to cancel later
            val sharedPrefs = context.getSharedPreferences("com.example.bcv_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putInt("pref_active_alarms_count", times.size).apply()
        }

        fun scheduleRandomReminders(context: Context, frequency: Int) {
            cancelAllReminders(context)
            val sharedPrefs = context.getSharedPreferences("com.example.bcv_prefs", Context.MODE_PRIVATE)
            
            // Generate stable random times for today
            val random = kotlin.random.Random(System.currentTimeMillis() + frequency)
            val times = mutableListOf<String>()
            for (i in 0 until frequency) {
                // Generate hour between 8 AM and 9 PM (inclusive) so it doesn't notify in the middle of the night
                val h = random.nextInt(8, 22)
                val m = random.nextInt(0, 60)
                times.add(String.format("%02d:%02d", h, m))
            }
            val sortedTimes = times.sorted()
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            sortedTimes.forEachIndexed { index, timeStr ->
                val parts = timeStr.split(":")
                if (parts.size != 2) return@forEachIndexed
                val hour = parts[0].toIntOrNull() ?: 9
                val minute = parts[1].toIntOrNull() ?: 0

                val intent = Intent(context, BcvReminderReceiver::class.java)
                val requestCode = 3501 + index
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                try {
                    alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                    Log.d("BcvReminderReceiver", "Random Alarm scheduled daily at $hour:$minute with code $requestCode. Next trigger: ${calendar.time}")
                } catch (e: Exception) {
                    Log.e("BcvReminderReceiver", "Failed to schedule random alarm", e)
                }
            }
            
            sharedPrefs.edit()
                .putInt("pref_active_random_alarms_count", sortedTimes.size)
                .putString("pref_generated_random_times_list", sortedTimes.joinToString(","))
                .apply()
        }

        fun cancelAllReminders(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, BcvReminderReceiver::class.java)
            
            // Cancel any old single alarm
            val legacyPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (legacyPendingIntent != null) {
                alarmManager.cancel(legacyPendingIntent)
                legacyPendingIntent.cancel()
            }

            val sharedPrefs = context.getSharedPreferences("com.example.bcv_prefs", Context.MODE_PRIVATE)
            val customCount = sharedPrefs.getInt("pref_active_alarms_count", 100)
            for (i in 0 until maxOf(customCount, 100)) {
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    3001 + i,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }

            val randomCount = sharedPrefs.getInt("pref_active_random_alarms_count", 100)
            for (i in 0 until maxOf(randomCount, 100)) {
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    3501 + i,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }
            Log.d("BcvReminderReceiver", "All scheduled alarms cancelled")
        }

        fun cancelReminder(context: Context) {
            cancelAllReminders(context)
        }
    }
}
