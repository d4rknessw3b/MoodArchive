package com.moodarchive.presentation.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moodarchive.domain.model.DiaryEntry
import com.moodarchive.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CalendarUiState(
    val selectedDate: Long = System.currentTimeMillis(),
    val entriesForDate: List<DiaryEntry> = emptyList(),
    val datesWithEntries: List<Long> = emptyList(),
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val isLoading: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: DiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadDatesWithEntries()
        selectDate(System.currentTimeMillis())
    }

    private fun loadDatesWithEntries() {
        viewModelScope.launch {
            repository.getDatesWithEntries().collect { dates ->
                _uiState.value = _uiState.value.copy(datesWithEntries = dates)
            }
        }
    }

    fun selectDate(timestamp: Long) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        _uiState.value = _uiState.value.copy(selectedDate = timestamp, isLoading = true)

        viewModelScope.launch {
            repository.getEntriesByDate(startOfDay, endOfDay).collect { entries ->
                _uiState.value = _uiState.value.copy(
                    entriesForDate = entries,
                    isLoading = false
                )
            }
        }
    }

    fun changeMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _uiState.value.currentYear)
            set(Calendar.MONTH, _uiState.value.currentMonth)
            add(Calendar.MONTH, delta)
        }
        _uiState.value = _uiState.value.copy(
            currentMonth = cal.get(Calendar.MONTH),
            currentYear = cal.get(Calendar.YEAR)
        )
    }
}
