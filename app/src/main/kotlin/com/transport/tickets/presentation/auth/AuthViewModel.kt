package com.transport.tickets.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transport.tickets.data.preferences.AppPreferences
import com.transport.tickets.data.remote.datasource.RemoteDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data object Success : AuthState
    data class Error(val message: String) : AuthState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val remoteDataSource: RemoteDataSource,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    val isLoggedIn: Boolean get() = runBlocking { appPreferences.userToken.first() != null }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val response = remoteDataSource.login(email.trim(), password)
                appPreferences.saveAuth(response.token, response.userId, response.email, response.createdAt)
                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Ошибка входа")
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val response = remoteDataSource.register(email.trim(), password)
                appPreferences.saveAuth(response.token, response.userId, response.email, response.createdAt)
                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Ошибка регистрации")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            appPreferences.clearAuth()
            _state.value = AuthState.Idle
        }
    }

    fun resetState() { _state.value = AuthState.Idle }
}
