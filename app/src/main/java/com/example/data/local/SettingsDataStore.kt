package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val decimalPrecision: Int = 8,
    val maxFractionDigits: Int = 12,
    val hapticFeedbackEnabled: Boolean = true
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "converter_td_settings")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DECIMAL_PRECISION = intPreferencesKey("decimal_precision")
        val MAX_FRACTION_DIGITS = intPreferencesKey("max_fraction_digits")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeName) }.getOrDefault(ThemeMode.SYSTEM)
        val decimalPrecision = (prefs[Keys.DECIMAL_PRECISION] ?: 8).coerceIn(2, 16)
        val maxFractionDigits = (prefs[Keys.MAX_FRACTION_DIGITS] ?: 12).coerceIn(4, 32)
        val haptic = prefs[Keys.HAPTIC_FEEDBACK] ?: true
        AppSettings(
            themeMode = themeMode,
            decimalPrecision = decimalPrecision,
            maxFractionDigits = maxFractionDigits,
            hapticFeedbackEnabled = haptic
        )
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateDecimalPrecision(precision: Int) {
        context.dataStore.edit {
            val clamped = precision.coerceIn(2, 16)
            it[Keys.DECIMAL_PRECISION] = clamped
            val currentMax = it[Keys.MAX_FRACTION_DIGITS] ?: 12
            if (currentMax < clamped) {
                it[Keys.MAX_FRACTION_DIGITS] = clamped
            }
        }
    }

    suspend fun updateMaxFractionDigits(maxDigits: Int) {
        context.dataStore.edit {
            val clamped = maxDigits.coerceIn(4, 32)
            it[Keys.MAX_FRACTION_DIGITS] = clamped
            val currentPrecision = it[Keys.DECIMAL_PRECISION] ?: 8
            if (currentPrecision > clamped) {
                it[Keys.DECIMAL_PRECISION] = clamped
            }
        }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun resetSettings() {
        context.dataStore.edit { it.clear() }
    }
}
