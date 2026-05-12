package com.moodarchive.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moodarchive.domain.model.DiaryEntry
import com.moodarchive.domain.model.Emotion
import com.moodarchive.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<DiaryEntry> = emptyList(),
    val filterEmotion: Emotion? = null,
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: DiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        // Debounce поиска — 300мс
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            performSearch()
        }
    }

    fun setEmotionFilter(emotion: Emotion?) {
        _uiState.value = _uiState.value.copy(filterEmotion = emotion)
        performSearch()
    }

    private fun performSearch() {
        val state = _uiState.value
        if (state.query.isBlank() && state.filterEmotion == null) {
            _uiState.value = state.copy(results = emptyList(), hasSearched = false)
            return
        }

        _uiState.value = state.copy(isSearching = true)

        viewModelScope.launch {
            if (state.query.isNotBlank()) {
                repository.searchEntries(state.query).collect { entries ->
                    val filtered = if (state.filterEmotion != null) {
                        entries.filter { it.emotion == state.filterEmotion }
                    } else {
                        entries
                    }
                    _uiState.value = _uiState.value.copy(
                        results = filtered,
                        isSearching = false,
                        hasSearched = true
                    )
                }
            } else if (state.filterEmotion != null) {
                repository.getEntriesByEmotion(state.filterEmotion).collect { entries ->
                    _uiState.value = _uiState.value.copy(
                        results = entries,
                        isSearching = false,
                        hasSearched = true
                    )
                }
            }
        }
    }
}
