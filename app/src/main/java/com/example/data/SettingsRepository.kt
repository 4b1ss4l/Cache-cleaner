package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import com.example.model.AutoCleanFrequency
import com.example.model.AutoCleanTarget
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cache_cleaner_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_CLEAN_FREQ = stringPreferencesKey("auto_clean_freq")
        val AUTO_CLEAN_HOUR = intPreferencesKey("auto_clean_hour")
        val AUTO_CLEAN_MIN = intPreferencesKey("auto_clean_min")
        val AUTO_CLEAN_TARGET = stringPreferencesKey("auto_clean_target")
        val AUTO_CLEAN_CONFIRM = booleanPreferencesKey("auto_clean_confirm")
        val NOTIFS_ENABLED = booleanPreferencesKey("notifs_enabled")
        val NOTIF_SCAN = booleanPreferencesKey("notif_scan")
        val NOTIF_CLEAN = booleanPreferencesKey("notif_clean")
        val SCAN_ON_OPEN = booleanPreferencesKey("scan_on_open")
        val AUTO_REFRESH = booleanPreferencesKey("auto_refresh")
        val SHOW_SYSTEM_APPS = booleanPreferencesKey("show_system_apps")
        val SHOW_TECH_INFO = booleanPreferencesKey("show_tech_info")
        val AUTO_CLEAR_DAYS = intPreferencesKey("auto_clear_days")
        val LAST_AUTO_CLEAN_TIME = longPreferencesKey("last_auto_clean_time")
        val LAST_AUTO_CLEAN_RES = stringPreferencesKey("last_auto_clean_res")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val themeModeStr = prefs[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.SYSTEM)

        val freqStr = prefs[PreferencesKeys.AUTO_CLEAN_FREQ] ?: AutoCleanFrequency.OFF.name
        val frequency = runCatching { AutoCleanFrequency.valueOf(freqStr) }.getOrDefault(AutoCleanFrequency.OFF)

        val targetStr = prefs[PreferencesKeys.AUTO_CLEAN_TARGET] ?: AutoCleanTarget.BOTH.name
        val target = runCatching { AutoCleanTarget.valueOf(targetStr) }.getOrDefault(AutoCleanTarget.BOTH)

        AppSettings(
            themeMode = themeMode,
            autoCleanFrequency = frequency,
            autoCleanHour = prefs[PreferencesKeys.AUTO_CLEAN_HOUR] ?: 3,
            autoCleanMinute = prefs[PreferencesKeys.AUTO_CLEAN_MIN] ?: 0,
            autoCleanTarget = target,
            autoCleanRequireConfirmation = prefs[PreferencesKeys.AUTO_CLEAN_CONFIRM] ?: false,
            notificationsEnabled = prefs[PreferencesKeys.NOTIFS_ENABLED] ?: true,
            notifyOnScanComplete = prefs[PreferencesKeys.NOTIF_SCAN] ?: true,
            notifyOnCleanComplete = prefs[PreferencesKeys.NOTIF_CLEAN] ?: true,
            scanOnAppOpen = prefs[PreferencesKeys.SCAN_ON_OPEN] ?: true,
            autoRefreshResults = prefs[PreferencesKeys.AUTO_REFRESH] ?: false,
            showSystemApps = prefs[PreferencesKeys.SHOW_SYSTEM_APPS] ?: true,
            showTechnicalInfo = prefs[PreferencesKeys.SHOW_TECH_INFO] ?: false,
            autoClearHistoryDays = prefs[PreferencesKeys.AUTO_CLEAR_DAYS] ?: 30,
            lastAutoCleanTimestamp = prefs[PreferencesKeys.LAST_AUTO_CLEAN_TIME] ?: 0L,
            lastAutoCleanResult = prefs[PreferencesKeys.LAST_AUTO_CLEAN_RES] ?: ""
        )
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateAutoCleanSettings(
        frequency: AutoCleanFrequency,
        hour: Int,
        minute: Int,
        target: AutoCleanTarget,
        requireConfirmation: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_CLEAN_FREQ] = frequency.name
            prefs[PreferencesKeys.AUTO_CLEAN_HOUR] = hour
            prefs[PreferencesKeys.AUTO_CLEAN_MIN] = minute
            prefs[PreferencesKeys.AUTO_CLEAN_TARGET] = target.name
            prefs[PreferencesKeys.AUTO_CLEAN_CONFIRM] = requireConfirmation
        }
    }

    suspend fun updateNotifications(enabled: Boolean, scan: Boolean, clean: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.NOTIFS_ENABLED] = enabled
            prefs[PreferencesKeys.NOTIF_SCAN] = scan
            prefs[PreferencesKeys.NOTIF_CLEAN] = clean
        }
    }

    suspend fun updateGeneralSettings(
        scanOnOpen: Boolean,
        autoRefresh: Boolean,
        showSystemApps: Boolean,
        showTechInfo: Boolean,
        autoClearDays: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SCAN_ON_OPEN] = scanOnOpen
            prefs[PreferencesKeys.AUTO_REFRESH] = autoRefresh
            prefs[PreferencesKeys.SHOW_SYSTEM_APPS] = showSystemApps
            prefs[PreferencesKeys.SHOW_TECH_INFO] = showTechInfo
            prefs[PreferencesKeys.AUTO_CLEAR_DAYS] = autoClearDays
        }
    }

    suspend fun recordAutoCleanResult(timestamp: Long, resultSummary: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_AUTO_CLEAN_TIME] = timestamp
            prefs[PreferencesKeys.LAST_AUTO_CLEAN_RES] = resultSummary
        }
    }

    suspend fun restoreDefaults() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
