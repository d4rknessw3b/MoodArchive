package com.moodarchive.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moodarchive.domain.model.DiaryEntry
import com.moodarchive.domain.model.Emotion
import com.moodarchive.domain.repository.DiaryRepository
import com.moodarchive.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val selectedEmotion: Emotion? = null,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DiaryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
        checkAutoSync()
    }

    private fun checkAutoSync() {
        viewModelScope.launch {
            try {
                if (settingsRepository.isAutoSyncEnabled.first()) {
                    repository.syncPendingEntries()
                }
            } catch (e: Exception) {
                // Ignore sync errors on silent startup
            }
        }
    }

    private fun loadEntries() {
        viewModelScope.launch {
            repository.getAllEntries().collect { entries ->
                _uiState.value = _uiState.value.copy(
                    entries = entries,
                    isLoading = false
                )
            }
        }
    }

    fun filterByEmotion(emotion: Emotion?) {
        _uiState.value = _uiState.value.copy(selectedEmotion = emotion, isLoading = true)
        viewModelScope.launch {
            if (emotion == null) {
                repository.getAllEntries().collect { entries ->
                    _uiState.value = _uiState.value.copy(entries = entries, isLoading = false)
                }
            } else {
                repository.getEntriesByEmotion(emotion).collect { entries ->
                    _uiState.value = _uiState.value.copy(entries = entries, isLoading = false)
                }
            }
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            repository.deleteEntry(id)
        }
    }
}
