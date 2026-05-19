package com.transport.tickets.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transport.tickets.data.remote.dto.RouteDto
import com.transport.tickets.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminPanelState(
    val isLoading: Boolean = false,
    val routes: List<RouteDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AdminPanelViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminPanelState())
    val state: StateFlow<AdminPanelState> = _state.asStateFlow()

    init { loadRoutes() }

    fun loadRoutes() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val routes = adminRepository.getAllRoutes()
                _state.value = _state.value.copy(isLoading = false, routes = routes)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun deleteRoute(id: Int) {
        viewModelScope.launch {
            try {
                adminRepository.deleteRoute(id)
                loadRoutes()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun logout(onLogout: () -> Unit) {
        adminRepository.logout()
        onLogout()
    }
}
