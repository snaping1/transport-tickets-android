package com.transport.tickets.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transport.tickets.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AdminLoginState {
    data object Idle : AdminLoginState
    data object Loading : AdminLoginState
    data object Success : AdminLoginState
    data class Error(val message: String) : AdminLoginState
}

@HiltViewModel
class AdminLoginViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AdminLoginState>(AdminLoginState.Idle)
    val state: StateFlow<AdminLoginState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AdminLoginState.Loading
            try {
                adminRepository.login(email, password)
                _state.value = AdminLoginState.Success
            } catch (e: Exception) {
                _state.value = AdminLoginState.Error(e.message ?: "Неверный логин или пароль")
            }
        }
    }

    fun resetState() { _state.value = AdminLoginState.Idle }
}
