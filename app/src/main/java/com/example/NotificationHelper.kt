package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "bcv_rate_alerts"
    const val DAILY_REMINDER_CHANNEL_ID = "bcv_daily_reminders"
    const val NOTIFICATION_ID_RATE = 1001
    const val NOTIFICATION_ID_REMINDER = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Tasas"
            val descriptionText = "Notificaciones instantáneas de cambios en la tasa del BCV"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val reminderName = "Recordatorios Diarios"
            val reminderDesc = "Recordatorios programados del valor actual del dólar"
            val reminderChannel = NotificationChannel(DAILY_REMINDER_CHANNEL_ID, reminderName, importance).apply {
                description = reminderDesc
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    fun showRateChangeNotification(context: Context, oldRate: Double, newRate: Double, dateStr: String) {
        val diff = newRate - oldRate
        val percent = if (oldRate > 0) (diff / oldRate) * 100 else 0.0
        val symbol = if (diff >= 0) "▲" else "▼"
        val sign = if (diff >= 0) "+" else ""
        
        val title = "Nueva Tasa BCV Oficial"
        val message = "USD: Bs. ${String.format("%.2f", newRate).replace(".", ",")} ($symbol $sign${String.format("%.2f", percent).replace(".", ",")}%)\nReflejo de cotización: $dateStr"

        showNotification(context, title, message, CHANNEL_ID, NOTIFICATION_ID_RATE)
    }

    fun showDailyReminderNotification(context: Context, rateUsd: Double, rateEur: Double, dateStr: String) {
        val title = "Monitor BCV: Recordatorio Diario"
        val message = "Dólar: Bs. ${String.format("%.2f", rateUsd).replace(".", ",")}\nEuro: Bs. ${String.format("%.2f", rateEur).replace(".", ",")}\nFecha oficial: $dateStr"

        showNotification(context, title, message, DAILY_REMINDER_CHANNEL_ID, NOTIFICATION_ID_REMINDER)
    }

    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String,
        notificationId: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Using standard Android system icon, since we don't have custom drawable asset
        val iconRes = android.R.drawable.ic_dialog_info

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // Even if permissions are lacked on API 33+, we catch or depend on permission checks at calling levels.
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
