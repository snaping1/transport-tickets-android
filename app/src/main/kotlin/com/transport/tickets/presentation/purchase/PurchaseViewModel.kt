package com.transport.tickets.presentation.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transport.tickets.domain.model.Ticket
import com.transport.tickets.domain.usecase.BuyTicketUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PurchaseState {
    data object Idle : PurchaseState
    data object Loading : PurchaseState
    data class Success(val ticket: Ticket) : PurchaseState
    data class Error(val message: String) : PurchaseState
}

enum class WagonType(val label: String, val shortLabel: String, val seatsPerWagon: Int, val priceMultiplier: Double) {
    SV("СВ (Люкс)", "СВ", 18, 2.5),
    COUPE("Купе", "Купе", 36, 1.8),
    PLATZKART("Плацкарт", "Плц", 54, 1.0),
    SEAT("Сидячий", "Сид", 60, 0.7)
}

data class WagonConfig(val number: Int, val type: WagonType)

// Состав поезда: 1 СВ + 4 Купе + 8 Плацкарт + 1 Сидячий = 14 вагонов, 654 места
val TRAIN_WAGONS: List<WagonConfig> = buildList {
    add(WagonConfig(1, WagonType.SV))
    addAll((2..5).map { WagonConfig(it, WagonType.COUPE) })
    addAll((6..13).map { WagonConfig(it, WagonType.PLATZKART) })
    add(WagonConfig(14, WagonType.SEAT))
}

data class PurchaseUiState(
    val occupiedSeats: List<Int> = emptyList(),
    val selectedSeats: Set<Int> = emptySet(),
    val seatsLoading: Boolean = false,
    val selectedWagon: Int = 1,
    val purchaseState: PurchaseState = PurchaseState.Idle
) {
    val seatCount: Int get() = selectedSeats.size

    val wagonType: WagonType
        get() = TRAIN_WAGONS.find { it.number == selectedWagon }?.type ?: WagonType.COUPE

    fun currentSeatRange(): IntRange {
        var start = 1
        for (w in TRAIN_WAGONS) {
            val end = start + w.type.seatsPerWagon - 1
            if (w.number == selectedWagon) return start..end
            start += w.type.seatsPerWagon
        }
        return 1..wagonType.seatsPerWagon
    }
}

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val buyTicketUseCase: BuyTicketUseCase,
    private val purchaseFlowHolder: PurchaseFlowHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState.asStateFlow()

    fun loadSeats(routeId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(seatsLoading = true) }
            try {
                val occupied = buyTicketUseCase.getOccupiedSeats(routeId)
                _uiState.update { it.copy(occupiedSeats = occupied, seatsLoading = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(seatsLoading = false) }
            }
        }
    }

    fun setWagon(wagon: Int) {
        _uiState.update { it.copy(selectedWagon = wagon, selectedSeats = emptySet()) }
    }

    fun toggleSeat(seat: Int) {
        _uiState.update { state ->
            val selected = state.selectedSeats.toMutableSet()
            if (seat in selected) selected.remove(seat)
            else if (selected.size < 6) selected.add(seat)
            state.copy(selectedSeats = selected)
        }
    }

    fun buyTicket(routeId: Int) {
        val state = _uiState.value
        if (state.selectedSeats.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(purchaseState = PurchaseState.Loading) }
            try {
                val seats = state.selectedSeats.sorted()
                val ticket = buyTicketUseCase(routeId, seats.size, seats)
                _uiState.update { it.copy(purchaseState = PurchaseState.Success(ticket)) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(purchaseState = PurchaseState.Error(e.message ?: "Ошибка покупки"))
                }
            }
        }
    }

    fun prepareForPassengerInput(route: com.transport.tickets.domain.model.Route) {
        val seats = _uiState.value.selectedSeats.toList().sorted()
        purchaseFlowHolder.route = route
        purchaseFlowHolder.selectedSeats = seats
        purchaseFlowHolder.passengers.clear()
    }

    fun resetState() = _uiState.update { it.copy(purchaseState = PurchaseState.Idle) }
}
