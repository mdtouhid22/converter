package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettings
import com.example.data.local.HistoryEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.ThemeMode
import com.example.data.repository.ConverterRepository
import com.example.domain.math.ConverterEngine
import com.example.domain.model.ConversionResult
import com.example.domain.model.NumberSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConverterUiState(
    val inputText: String = "",
    val sourceSystem: NumberSystem = NumberSystem.BINARY,
    val targetSystem: NumberSystem = NumberSystem.DECIMAL,
    val liveError: String? = null,
    val submitError: String? = null,
    val currentResult: ConversionResult? = null,
    val currentHistoryId: Long? = null,
    val currentIsFavorite: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = ConverterEngine()
    private val repository: ConverterRepository = run {
        val db = AppDatabase.getInstance(application)
        val settingsStore = SettingsDataStore(application)
        ConverterRepository(db.historyDao(), settingsStore)
    }

    private val _uiState = MutableStateFlow(ConverterUiState())
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val allHistory: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory: StateFlow<List<HistoryEntity>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteHistory: StateFlow<List<HistoryEntity>> = repository.favoriteHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onInputChange(newValue: String) {
        val source = _uiState.value.sourceSystem
        val liveErr = engine.validateLiveInput(newValue, source)
        _uiState.update {
            it.copy(
                inputText = newValue,
                liveError = liveErr,
                submitError = null
            )
        }
    }

    fun clearInput() {
        _uiState.update {
            it.copy(
                inputText = "",
                liveError = null,
                submitError = null,
                currentResult = null,
                currentHistoryId = null,
                currentIsFavorite = false
            )
        }
    }

    fun onSourceSystemSelected(system: NumberSystem) {
        _uiState.update { state ->
            val liveErr = engine.validateLiveInput(state.inputText, system)
            state.copy(
                sourceSystem = system,
                liveError = liveErr,
                submitError = null
            )
        }
    }

    fun onTargetSystemSelected(system: NumberSystem) {
        _uiState.update { it.copy(targetSystem = system, submitError = null) }
    }

    fun swapSystems() {
        _uiState.update { state ->
            val newSource = state.targetSystem
            val newTarget = state.sourceSystem
            val liveErr = engine.validateLiveInput(state.inputText, newSource)
            state.copy(
                sourceSystem = newSource,
                targetSystem = newTarget,
                liveError = liveErr,
                submitError = null
            )
        }
    }

    fun convert() {
        val state = _uiState.value
        val currentSettings = settings.value
        val outcome = engine.convert(
            rawInput = state.inputText,
            source = state.sourceSystem,
            target = state.targetSystem,
            decimalPrecision = currentSettings.decimalPrecision,
            maxFractionDigits = currentSettings.maxFractionDigits
        )

        outcome.onSuccess { result ->
            _uiState.update {
                it.copy(
                    liveError = null,
                    submitError = null,
                    currentResult = result
                )
            }
            viewModelScope.launch {
                val saved = repository.saveConversion(result)
                _uiState.update {
                    it.copy(
                        currentHistoryId = saved.id,
                        currentIsFavorite = saved.isFavorite
                    )
                }
            }
        }.onFailure { err ->
            _uiState.update {
                it.copy(
                    submitError = err.message ?: "Invalid input for ${state.sourceSystem.displayName}."
                )
            }
        }
    }

    fun useResultAsInput() {
        val result = _uiState.value.currentResult ?: return
        val newSource = result.targetSystem
        val newTarget = result.sourceSystem
        _uiState.update {
            it.copy(
                inputText = result.convertedValue,
                sourceSystem = newSource,
                targetSystem = newTarget,
                liveError = null,
                submitError = null
            )
        }
        convert()
    }

    fun loadConversionAndConvert(
        input: String,
        source: NumberSystem,
        target: NumberSystem
    ) {
        _uiState.update {
            it.copy(
                inputText = input,
                sourceSystem = source,
                targetSystem = target,
                liveError = null,
                submitError = null
            )
        }
        convert()
    }

    fun loadFromHistory(entity: HistoryEntity) {
        val source = NumberSystem.fromBase(entity.sourceBase)
        val target = NumberSystem.fromBase(entity.targetBase)
        loadConversionAndConvert(entity.inputValue, source, target)
    }

    fun toggleCurrentFavorite() {
        val id = _uiState.value.currentHistoryId ?: return
        val currentlyFav = _uiState.value.currentIsFavorite
        _uiState.update { it.copy(currentIsFavorite = !currentlyFav) }
        viewModelScope.launch {
            repository.toggleFavorite(id, currentlyFav)
        }
    }

    fun toggleHistoryFavorite(entity: HistoryEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(entity.id, entity.isFavorite)
            if (_uiState.value.currentHistoryId == entity.id) {
                _uiState.update { it.copy(currentIsFavorite = !entity.isFavorite) }
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
            if (_uiState.value.currentHistoryId == id) {
                _uiState.update { it.copy(currentHistoryId = null, currentIsFavorite = false) }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.update { it.copy(currentHistoryId = null, currentIsFavorite = false) }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.updateThemeMode(mode) }
    }

    fun updateDecimalPrecision(precision: Int) {
        viewModelScope.launch {
            repository.updateDecimalPrecision(precision)
        }
    }

    fun updateMaxFractionDigits(maxDigits: Int) {
        viewModelScope.launch {
            repository.updateMaxFractionDigits(maxDigits)
        }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { repository.updateHapticFeedback(enabled) }
    }

    fun resetSettings() {
        viewModelScope.launch { repository.resetSettings() }
    }
}
