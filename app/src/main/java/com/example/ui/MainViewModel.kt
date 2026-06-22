package com.example.ui

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BcvRateRecord
import com.example.data.BcvRepository
import com.example.network.BcvScraper
import com.example.network.GeminiNetwork
import com.example.network.ScrapResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

class MainViewModel(
    private val repository: BcvRepository,
    private val sharedPreferences: SharedPreferences,
    private val context: Context
) : ViewModel() {

    private val TAG = "MainViewModel"

    // Theme Mode selection
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Raw rates history reactively from Room
    val ratesHistory: StateFlow<List<BcvRateRecord>> = repository.allRates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current displayed rate record
    private val _currentRate = MutableStateFlow<BcvRateRecord?>(null)
    val currentRate: StateFlow<BcvRateRecord?> = _currentRate.asStateFlow()

    // Sync status states
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Variation indicator based on previous items
    private val _variationPercent = MutableStateFlow("0,00%")
    val variationPercent: StateFlow<String> = _variationPercent.asStateFlow()

    private val _isVariationPositive = MutableStateFlow(true)
    val isVariationPositive: StateFlow<Boolean> = _isVariationPositive.asStateFlow()

    // --- Converter States ---
    private val _converterInput = MutableStateFlow("")
    val converterInput: StateFlow<String> = _converterInput.asStateFlow()

    private val _converterOutput = MutableStateFlow("0,00")
    val converterOutput: StateFlow<String> = _converterOutput.asStateFlow()

    private val _isConvertingUsdToBs = MutableStateFlow(true)
    val isConvertingUsdToBs: StateFlow<Boolean> = _isConvertingUsdToBs.asStateFlow()

    // --- Notification Preferences ---
    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notifyOnChanges = MutableStateFlow(false)
    val notifyOnChanges: StateFlow<Boolean> = _notifyOnChanges.asStateFlow()

    private val _dailyReminderEnabled = MutableStateFlow(false)
    val dailyReminderEnabled: StateFlow<Boolean> = _dailyReminderEnabled.asStateFlow()

    private val _dailyReminderTime = MutableStateFlow("09:00")
    val dailyReminderTime: StateFlow<String> = _dailyReminderTime.asStateFlow()

    private val _autoRefreshEnabled = MutableStateFlow(false)
    val autoRefreshEnabled: StateFlow<Boolean> = _autoRefreshEnabled.asStateFlow()

    init {
        // Load theme preference
        val savedTheme = sharedPreferences.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        _themeMode.value = try {
            ThemeMode.valueOf(savedTheme)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }

        // Load notifications configuration
        _notificationsEnabled.value = sharedPreferences.getBoolean("pref_notif_enabled", false)
        _notifyOnChanges.value = sharedPreferences.getBoolean("pref_notif_changes", false)
        _dailyReminderEnabled.value = sharedPreferences.getBoolean("pref_daily_reminder_enabled", false)
        _dailyReminderTime.value = sharedPreferences.getString("pref_daily_reminder_time", "09:00") ?: "09:00"
        _autoRefreshEnabled.value = sharedPreferences.getBoolean("pref_auto_refresh_enabled", false)

        // Initialize background worker if enabled
        if (_autoRefreshEnabled.value) {
            com.example.BcvSyncScheduler.scheduleBackgroundSync(context)
        }

        // Load latest available record initially
        loadInitialData()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        sharedPreferences.edit().putString("theme_mode", mode.name).apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        sharedPreferences.edit().putBoolean("pref_notif_enabled", enabled).apply()
        
        // Handle alarm update
        if (!enabled) {
            com.example.BcvReminderReceiver.cancelReminder(context)
        } else if (_dailyReminderEnabled.value) {
            com.example.BcvReminderReceiver.scheduleReminder(context, _dailyReminderTime.value)
        }
    }

    fun setNotifyOnChanges(enabled: Boolean) {
        _notifyOnChanges.value = enabled
        sharedPreferences.edit().putBoolean("pref_notif_changes", enabled).apply()
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        _dailyReminderEnabled.value = enabled
        sharedPreferences.edit().putBoolean("pref_daily_reminder_enabled", enabled).apply()
        
        if (enabled && _notificationsEnabled.value) {
            com.example.BcvReminderReceiver.scheduleReminder(context, _dailyReminderTime.value)
        } else {
            com.example.BcvReminderReceiver.cancelReminder(context)
        }
    }

    fun setDailyReminderTime(timeStr: String) {
        _dailyReminderTime.value = timeStr
        sharedPreferences.edit().putString("pref_daily_reminder_time", timeStr).apply()
        
        if (_dailyReminderEnabled.value && _notificationsEnabled.value) {
            com.example.BcvReminderReceiver.scheduleReminder(context, timeStr)
        }
    }

    fun setAutoRefreshEnabled(enabled: Boolean) {
        _autoRefreshEnabled.value = enabled
        sharedPreferences.edit().putBoolean("pref_auto_refresh_enabled", enabled).apply()
        
        if (enabled) {
            com.example.BcvSyncScheduler.scheduleBackgroundSync(context)
        } else {
            com.example.BcvSyncScheduler.cancelBackgroundSync(context)
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val latest = repository.getLatestRate()
            if (latest != null) {
                _currentRate.value = latest
                calculateVariation(latest)
                _isRefreshing.value = false
            } else {
                // If DB is fresh/empty, trigger rates fetch
                refreshRates()
            }
        }
    }

    fun refreshRates() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            Log.d(TAG, "Starting refresh rates process")

            // Phase 1: Try scraping bcv.org.ve directly
            val scrapRes = BcvScraper.fetchRatesDirectly()
            if (scrapRes.success) {
                saveScrapedRates(scrapRes, "BCV Oficial")
            } else {
                // Phase 2: If Direct fails, try Gemini AI Rate Recovery
                Log.w(TAG, "Scraper failed: ${scrapRes.errorMsg}. Invoking Gemini AI fallback.")
                val aiRes = GeminiNetwork.fetchRatesThroughAi()
                if (aiRes.success) {
                    saveScrapedRates(aiRes, "Asistente IA")
                } else {
                    // Phase 3: Both failed, fallback to local DB or sandbox default
                    val lastSaved = repository.getLatestRate()
                    if (lastSaved != null) {
                        _currentRate.value = lastSaved
                        calculateVariation(lastSaved)
                        _errorMessage.value = "No se pudo actualizar de red. Mostrando datos sin conexión."
                    } else {
                        // DB empty and offline, load template defaults
                        val todayString = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "VE")).format(Date())
                        val sandboxRecord = BcvRateRecord(
                            dateText = todayString,
                            usd = 36.42,
                            eur = 39.55,
                            cny = 5.01,
                            tryLira = 1.13,
                            rub = 0.39,
                            source = "Simulado"
                        )
                        repository.insertRate(sandboxRecord)
                        _currentRate.value = sandboxRecord
                        _variationPercent.value = "+0,12%"
                        _isVariationPositive.value = true
                        _errorMessage.value = "Modo demostración. Conéctate a una red para sincronizar."
                        
                        // Notify Home Screen widget
                        try {
                            com.example.BcvAppWidgetProvider.triggerUpdate(context)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error notifying widget", e)
                        }
                    }
                }
            }
            _isRefreshing.value = false
            updateConversion()
        }
    }

    private suspend fun saveScrapedRates(res: ScrapResult, source: String) {
        val oldRecord = _currentRate.value
        val nextRecord = BcvRateRecord(
            dateText = res.dateText.ifEmpty {
                SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "VE")).format(Date())
            },
            usd = res.usd,
            eur = res.eur,
            cny = res.cny,
            tryLira = res.tryLira,
            rub = res.rub,
            source = source
        )
        repository.insertRate(nextRecord)
        _currentRate.value = nextRecord
        calculateVariation(nextRecord)

        // Trigger rate change notification if settings allow and value actually changed
        if (_notificationsEnabled.value && _notifyOnChanges.value && oldRecord != null && oldRecord.usd != nextRecord.usd) {
            try {
                com.example.NotificationHelper.showRateChangeNotification(
                    context = context,
                    oldRate = oldRecord.usd,
                    newRate = nextRecord.usd,
                    dateStr = nextRecord.dateText
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send change notification", e)
            }
        }

        // Notify Home Screen widget
        try {
            com.example.BcvAppWidgetProvider.triggerUpdate(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying widget", e)
        }
    }

    private suspend fun calculateVariation(current: BcvRateRecord) {
        val previous = repository.getPreviousRate(current.id)
        if (previous != null && previous.usd > 0.0) {
            val diff = current.usd - previous.usd
            val percent = (diff / previous.usd) * 100.0
            val df = DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale("es", "VE")))
            val sign = if (percent >= 0) "+" else ""
            _variationPercent.value = "$sign${df.format(percent)}%"
            _isVariationPositive.value = percent >= 0
        } else {
            // Static fallback design variation
            _variationPercent.value = "+0,12%"
            _isVariationPositive.value = true
        }
    }

    // --- Converter Actions ---
    fun onConverterInputChange(input: String) {
        // Enforce decimal separators format
        val clean = input.replace(",", ".")
        if (clean.isEmpty() || clean.toDoubleOrNull() != null || clean == ".") {
            _converterInput.value = input
            updateConversion()
        }
    }

    fun toggleConversionDirection() {
        _isConvertingUsdToBs.value = !_isConvertingUsdToBs.value
        updateConversion()
    }

    private fun updateConversion() {
        val inputStr = _converterInput.value.replace(",", ".").trim()
        val usdRate = _currentRate.value?.usd ?: 36.42
        
        if (inputStr.isEmpty()) {
            _converterOutput.value = "0,00"
            return
        }

        val inputVal = inputStr.toDoubleOrNull() ?: 0.0
        val df = DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale("es", "VE")))
        
        if (_isConvertingUsdToBs.value) {
            val converted = inputVal * usdRate
            _converterOutput.value = df.format(converted)
        } else {
            val converted = if (usdRate > 0.0) inputVal / usdRate else 0.0
            _converterOutput.value = df.format(converted)
        }
    }

    fun clearRates() {
        viewModelScope.launch {
            repository.clearRates()
            _currentRate.value = null
            _variationPercent.value = "0,00%"

            // Notify Home Screen widget
            try {
                com.example.BcvAppWidgetProvider.triggerUpdate(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying widget", e)
            }
        }
    }
}

// Custom ViewModel Factory compatible with standard Composable injection
class ViewModelFactory(
    private val repository: BcvRepository,
    private val sharedPreferences: SharedPreferences,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, sharedPreferences, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
