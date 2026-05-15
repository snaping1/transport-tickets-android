package com.transport.tickets.presentation.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transport.tickets.domain.model.Ticket
import com.transport.tickets.domain.usecase.CancelTicketUseCase
import com.transport.tickets.domain.usecase.GetMyTicketsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyTicketsUiState(
    val tickets: List<Ticket> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val cancellingTicketId: Int? = null
)

@HiltViewModel
class MyTicketsViewModel @Inject constructor(
    private val getMyTicketsUseCase: GetMyTicketsUseCase,
    private val cancelTicketUseCase: CancelTicketUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyTicketsUiState())
    val uiState: StateFlow<MyTicketsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMyTicketsUseCase().collect { tickets ->
                _uiState.update { it.copy(tickets = tickets) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                getMyTicketsUseCase.refresh()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Нет подключения. Показаны кешированные данные.")
                }
            }
        }
    }

    fun cancelTicket(ticketId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(cancellingTicketId = ticketId) }
            try {
                cancelTicketUseCase(ticketId)
                _uiState.update { it.copy(cancellingTicketId = null) }
                refresh()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cancellingTicketId = null,
                        error = e.message ?: "Ошибка отмены билета"
                    )
                }
            }
        }
    }
}
