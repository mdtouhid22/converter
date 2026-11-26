package com.example.data.repository

import com.example.data.local.AppSettings
import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.ThemeMode
import com.example.domain.model.ConversionResult
import kotlinx.coroutines.flow.Flow

class ConverterRepository(
    private val historyDao: HistoryDao,
    private val settingsDataStore: SettingsDataStore
) {
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    val recentHistory: Flow<List<HistoryEntity>> = historyDao.getRecentHistory(limit = 5)
    val favoriteHistory: Flow<List<HistoryEntity>> = historyDao.getFavoriteHistory()
    val settings: Flow<AppSettings> = settingsDataStore.settingsFlow

    suspend fun saveConversion(result: ConversionResult): HistoryEntity {
        val existing = historyDao.findExisting(
            inputValue = result.normalizedInput,
            sourceBase = result.sourceSystem.base,
            targetBase = result.targetSystem.base
        )
        val entity = HistoryEntity(
            id = existing?.id ?: 0L,
            inputValue = result.normalizedInput,
            sourceBase = result.sourceSystem.base,
            targetBase = result.targetSystem.base,
            resultValue = result.convertedValue,
            isExact = result.isExact,
            isFavorite = existing?.isFavorite ?: false,
            timestamp = System.currentTimeMillis()
        )
        val insertedId = historyDao.insert(entity)
        return entity.copy(id = if (entity.id != 0L) entity.id else insertedId)
    }

    suspend fun toggleFavorite(id: Long, currentlyFavorite: Boolean) {
        historyDao.setFavorite(id, !currentlyFavorite)
    }

    suspend fun deleteHistoryItem(id: Long) {
        historyDao.deleteById(id)
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }

    suspend fun updateThemeMode(mode: ThemeMode) = settingsDataStore.updateThemeMode(mode)
    suspend fun updateDecimalPrecision(precision: Int) = settingsDataStore.updateDecimalPrecision(precision)
    suspend fun updateMaxFractionDigits(maxDigits: Int) = settingsDataStore.updateMaxFractionDigits(maxDigits)
    suspend fun updateHapticFeedback(enabled: Boolean) = settingsDataStore.updateHapticFeedback(enabled)
    suspend fun resetSettings() = settingsDataStore.resetSettings()
}
