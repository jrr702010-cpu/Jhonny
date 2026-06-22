package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.room.Room
import com.example.data.BcvDatabase
import com.example.data.BcvRateRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Locale

class BcvAppWidgetProvider : AppWidgetProvider() {

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d("BcvAppWidget", "onUpdate triggered")

        // Load the database asynchronously and update each active widget
        widgetScope.launch {
            try {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    BcvDatabase::class.java,
                    "bcv_rates_v2.db"
                ).fallbackToDestructiveMigration().build()
                
                val latestRate = db.bcvDao().getLatestRate()
                val previousRate = latestRate?.let { db.bcvDao().getPreviousRate(it.id) }

                db.close() // Close the db inside our scope

                appWidgetIds.forEach { widgetId ->
                    updateWidgetState(context, appWidgetManager, widgetId, latestRate, previousRate)
                }
            } catch (e: Exception) {
                Log.e("BcvAppWidget", "Error updating widget on room query", e)
            }
        }
    }

    private fun updateWidgetState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        latest: BcvRateRecord?,
        previous: BcvRateRecord?
    ) {
        val views = RemoteViews(context.packageName, R.layout.bcv_widget)

        // Setup default presentation if DB is currently empty
        val valueStr: String
        val dateStr: String
        val variationStr: String
        val isPositive: Boolean

        if (latest != null) {
            valueStr = String.format("%.2f", latest.usd).replace(".", ",")
            dateStr = "Actualizado: ${latest.dateText}"

            if (previous != null && previous.usd > 0.0) {
                val diff = latest.usd - previous.usd
                val percent = (diff / previous.usd) * 100.0
                val df = DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale("es", "VE")))
                val sign = if (percent >= 0) "+" else ""
                variationStr = "$sign${df.format(percent)}%"
                isPositive = percent >= 0
            } else {
                variationStr = "+0,12%"
                isPositive = true
            }
        } else {
            // Placeholder template display
            valueStr = "36,42"
            dateStr = "Sin conexión / cargando"
            variationStr = "+0,12%"
            isPositive = true
        }

        // Apply styled values to widgets views
        views.setTextViewText(R.id.widget_rate_value, valueStr)
        views.setTextViewText(R.id.widget_rate_date, dateStr)
        views.setTextViewText(
            R.id.widget_rate_variation,
            "${if (isPositive) "▲" else "▼"} $variationStr"
        )

        // Color coding matching green or red depending on movement
        val labelColor = context.getColor(
            if (isPositive) R.color.binance_green else R.color.binance_red
        )
        views.setTextColor(R.id.widget_rate_variation, labelColor)

        // Launch app upon click
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            widgetId,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // Instruct changes to manager
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, BcvAppWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, BcvAppWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
            Log.d("BcvAppWidget", "Triggered widget broadcast update for ${ids.size} widgets")
        }
    }
}
