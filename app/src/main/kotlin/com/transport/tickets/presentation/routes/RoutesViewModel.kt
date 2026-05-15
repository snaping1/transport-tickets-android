package com.transport.tickets.presentation.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transport.tickets.domain.model.Route
import com.transport.tickets.domain.usecase.GetRoutesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutesUiState(
    val routes: List<Route> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false,
    val originFilter: String = "",
    val destinationFilter: String = "",
    val dateFilter: String = "",
    val transportTypeFilter: String = ""
)

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val getRoutesUseCase: GetRoutesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    init {
        observeRoutes()
        refresh()
    }

    private fun observeRoutes() {
        viewModelScope.launch {
            _uiState.flatMapLatest { state ->
                getRoutesUseCase(
                    origin = state.originFilter.takeIf { it.isNotBlank() },
                    destination = state.destinationFilter.takeIf { it.isNotBlank() },
                    transportType = state.transportTypeFilter.takeIf { it.isNotBlank() }
                )
            }.collect { routes ->
                _uiState.update { it.copy(routes = routes) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val state = _uiState.value
                getRoutesUseCase.refresh(
                    origin = state.originFilter.takeIf { it.isNotBlank() },
                    destination = state.destinationFilter.takeIf { it.isNotBlank() },
                    date = state.dateFilter.takeIf { it.isNotBlank() },
                    transportType = state.transportTypeFilter.takeIf { it.isNotBlank() }
                )
                _uiState.update { it.copy(isLoading = false, isOffline = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isOffline = true,
                        error = "Нет подключения. Показаны кешированные данные."
                    )
                }
            }
        }
    }

    fun setOriginFilter(origin: String) = _uiState.update { it.copy(originFilter = origin) }
    fun setDestinationFilter(destination: String) = _uiState.update { it.copy(destinationFilter = destination) }
    fun setDateFilter(date: String) = _uiState.update { it.copy(dateFilter = date) }
    fun setTransportTypeFilter(type: String) {
        _uiState.update { it.copy(transportTypeFilter = type) }
        refresh()
    }
    fun applyFilters() = refresh()
}
