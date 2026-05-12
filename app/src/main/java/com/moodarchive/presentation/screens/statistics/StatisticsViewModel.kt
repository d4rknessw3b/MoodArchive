package com.moodarchive.presentation.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moodarchive.domain.model.Emotion
import com.moodarchive.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class StatisticsUiState(
    val emotionCounts: Map<Emotion, Int> = emptyMap(),
    val totalEntries: Int = 0,
    val topEmotion: Emotion? = null,
    val selectedPeriod: StatsPeriod = StatsPeriod.MONTH,
    val isLoading: Boolean = true
)

enum class StatsPeriod(val displayName: String) {
    WEEK("Неделя"),
    MONTH("Месяц"),
    YEAR("Год"),
    ALL("Всё время")
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: DiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStats(StatsPeriod.MONTH)
    }

    fun selectPeriod(period: StatsPeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period, isLoading = true)
        loadStats(period)
    }

    private fun loadStats(period: StatsPeriod) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        val startDate = when (period) {
            StatsPeriod.WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            StatsPeriod.MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.timeInMillis
            }
            StatsPeriod.YEAR -> {
                cal.add(Calendar.YEAR, -1)
                cal.timeInMillis
            }
            StatsPeriod.ALL -> 0L
        }

        viewModelScope.launch {
            repository.getEmotionStats(startDate, now).collect { stats ->
                val total = stats.values.sum()
                val top = stats.maxByOrNull { it.value }?.key
                _uiState.value = _uiState.value.copy(
                    emotionCounts = stats,
                    totalEntries = total,
                    topEmotion = top,
                    isLoading = false
                )
            }
        }
    }
}
