package com.transport.tickets.presentation.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transport.tickets.data.preferences.AppPreferences
import com.transport.tickets.domain.model.Route
import com.transport.tickets.domain.usecase.GetRoutesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchHistoryEntry(val origin: String, val destination: String, val date: String)

private fun String.toHistory(): List<SearchHistoryEntry> =
    split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
        val p = line.split("|")
        if (p.size == 3) SearchHistoryEntry(p[0], p[1], p[2]) else null
    }

private fun List<SearchHistoryEntry>.toPrefsString(): String =
    joinToString("\n") { "${it.origin}|${it.destination}|${it.date}" }

data class RoutesUiState(
    val routes: List<Route> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false,
    val originFilter: String = "",
    val destinationFilter: String = "",
    val dateFilter: String = "",
    val transportTypeFilter: String = "",
    val searchHistory: List<SearchHistoryEntry> = emptyList()
)

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val getRoutesUseCase: GetRoutesUseCase,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferences.searchHistory.collect { raw ->
                _uiState.update { it.copy(searchHistory = raw.toHistory()) }
            }
        }
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

    fun applyFilters() {
        val state = _uiState.value
        val origin = state.originFilter.trim()
        val destination = state.destinationFilter.trim()
        if (origin.isNotEmpty() || destination.isNotEmpty()) {
            viewModelScope.launch { saveToHistory(origin, destination, state.dateFilter) }
        }
        refresh()
    }

    fun applyHistoryEntry(entry: SearchHistoryEntry) {
        _uiState.update {
            it.copy(
                originFilter = entry.origin,
                destinationFilter = entry.destination,
                dateFilter = entry.date
            )
        }
        refresh()
    }

    fun clearHistory() {
        viewModelScope.launch { appPreferences.setSearchHistory("") }
    }

    private suspend fun saveToHistory(origin: String, destination: String, date: String) {
        val entry = SearchHistoryEntry(origin, destination, date)
        val current = _uiState.value.searchHistory
            .filter { it != entry }
            .toMutableList()
        current.add(0, entry)
        val trimmed = current.take(10)
        appPreferences.setSearchHistory(trimmed.toPrefsString())
    }
}
