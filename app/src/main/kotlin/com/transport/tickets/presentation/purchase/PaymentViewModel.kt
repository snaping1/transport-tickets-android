package com.transport.tickets.presentation.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transport.tickets.domain.model.Route
import com.transport.tickets.domain.usecase.BuyTicketUseCase
import com.transport.tickets.domain.usecase.SaveTicketPassengersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PaymentUiState {
    data object Idle : PaymentUiState
    data object Checking : PaymentUiState
    data object Processing : PaymentUiState
    data class Success(val ticketId: Int) : PaymentUiState
    data class Error(val message: String) : PaymentUiState
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val purchaseFlowHolder: PurchaseFlowHolder,
    private val buyTicketUseCase: BuyTicketUseCase,
    private val saveTicketPassengersUseCase: SaveTicketPassengersUseCase
) : ViewModel() {

    private val _paymentState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val paymentState: StateFlow<PaymentUiState> = _paymentState.asStateFlow()

    val route: Route? get() = purchaseFlowHolder.route
    val seats: List<Int> get() = purchaseFlowHolder.selectedSeats
    val totalPrice: Double get() = purchaseFlowHolder.totalPrice()

    fun pay() {
        val r = purchaseFlowHolder.route ?: return
        val seats = purchaseFlowHolder.selectedSeats
        val passengers = purchaseFlowHolder.passengers.toMap()

        viewModelScope.launch {
            _paymentState.value = PaymentUiState.Checking
            delay(1000)
            _paymentState.value = PaymentUiState.Processing
            delay(1500)
            try {
                val ticket = buyTicketUseCase(r.id, seats.size, seats, passengers)
                if (passengers.isNotEmpty()) {
                    saveTicketPassengersUseCase(ticket.id, passengers)
                }
                purchaseFlowHolder.clear()
                _paymentState.value = PaymentUiState.Success(ticket.id)
            } catch (e: Exception) {
                _paymentState.value = PaymentUiState.Error(e.message ?: "Ошибка оплаты")
            }
        }
    }

    fun resetError() {
        _paymentState.value = PaymentUiState.Idle
    }
}
