package com.moodarchive.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moodarchive.domain.repository.AuthRepository
import com.moodarchive.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun signIn() {
        val state = _uiState.value
        if (!validate(state)) return
        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = authRepository.signIn(state.email.trim(), state.password)
            result.fold(
                onSuccess = { 
                    diaryRepository.syncPendingEntries()
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true) 
                },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.localizedMessage) }
            )
        }
    }

    fun signUp() {
        val state = _uiState.value
        if (!validate(state)) return
        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = authRepository.signUp(state.email.trim(), state.password)
            result.fold(
                onSuccess = { 
                    diaryRepository.syncPendingEntries()
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true) 
                },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.localizedMessage) }
            )
        }
    }

    private fun validate(state: AuthUiState): Boolean {
        if (state.email.isBlank()) {
            _uiState.value = state.copy(error = "Введите email")
            return false
        }
        if (state.password.length < 6) {
            _uiState.value = state.copy(error = "Пароль должен быть не менее 6 символов")
            return false
        }
        return true
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
