package com.transport.tickets.presentation.tickets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transport.tickets.domain.model.Passenger
import com.transport.tickets.domain.model.Ticket
import com.transport.tickets.domain.usecase.CancelTicketUseCase
import com.transport.tickets.domain.usecase.GetMyTicketsUseCase
import com.transport.tickets.domain.usecase.GetTicketPassengersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketDetailUiState(
    val ticket: Ticket? = null,
    val passengers: List<Pair<Int, Passenger>> = emptyList(),
    val cancelling: Boolean = false,
    val cancelled: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TicketDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMyTicketsUseCase: GetMyTicketsUseCase,
    private val cancelTicketUseCase: CancelTicketUseCase,
    private val getTicketPassengersUseCase: GetTicketPassengersUseCase
) : ViewModel() {

    private val ticketId: Int = checkNotNull(savedStateHandle["ticketId"]).let {
        when (it) {
            is Int -> it
            is String -> it.toInt()
            else -> 0
        }
    }

    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMyTicketsUseCase().collect { tickets ->
                val ticket = tickets.find { it.id == ticketId }
                _uiState.update { it.copy(ticket = ticket) }
            }
        }
        viewModelScope.launch {
            try { getMyTicketsUseCase.refresh() } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try {
                val passengers = getTicketPassengersUseCase(ticketId)
                _uiState.update { it.copy(passengers = passengers) }
            } catch (_: Exception) {}
        }
    }

    fun cancelTicket() {
        viewModelScope.launch {
            _uiState.update { it.copy(cancelling = true, error = null) }
            try {
                cancelTicketUseCase(ticketId)
                _uiState.update { it.copy(cancelling = false, cancelled = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(cancelling = false, error = e.message ?: "Ошибка отмены") }
            }
        }
    }
}
