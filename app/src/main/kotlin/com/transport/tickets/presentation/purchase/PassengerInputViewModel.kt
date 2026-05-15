package com.transport.tickets.presentation.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transport.tickets.domain.model.Passenger
import com.transport.tickets.domain.model.Route
import com.transport.tickets.domain.usecase.GetPassengersUseCase
import com.transport.tickets.domain.usecase.SavePassengerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PassengerInputUiState(
    val seats: List<Int> = emptyList(),
    val route: Route? = null,
    val drafts: Map<Int, Passenger> = emptyMap(),
    val savedPassengers: List<Passenger> = emptyList(),
    val currentSeatIdx: Int = 0,
    val savingPassengerIdx: Int? = null,
    val showSavedPickerForIdx: Int? = null,
    val savedIndices: Set<Int> = emptySet()
) {
    fun draftFor(idx: Int): Passenger = drafts[idx] ?: Passenger()

    fun isValid(idx: Int): Boolean {
        val d = draftFor(idx)
        return d.firstName.isNotBlank() && d.lastName.isNotBlank() && d.documentNumber.isNotBlank()
    }

    val allValid: Boolean get() = seats.indices.all { isValid(it) }
}

@HiltViewModel
class PassengerInputViewModel @Inject constructor(
    private val purchaseFlowHolder: PurchaseFlowHolder,
    private val getPassengersUseCase: GetPassengersUseCase,
    private val savePassengerUseCase: SavePassengerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PassengerInputUiState())
    val uiState: StateFlow<PassengerInputUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                seats = purchaseFlowHolder.selectedSeats,
                route = purchaseFlowHolder.route
            )
        }
        viewModelScope.launch {
            getPassengersUseCase().collect { passengers ->
                _uiState.update { it.copy(savedPassengers = passengers) }
            }
        }
    }

    fun setCurrentSeat(idx: Int) = _uiState.update { it.copy(currentSeatIdx = idx) }

    fun updateDraft(idx: Int, passenger: Passenger) {
        _uiState.update { state ->
            // If the user edits the form after saving, re-enable the save button
            val newSaved = if (idx in state.savedIndices) state.savedIndices - idx else state.savedIndices
            state.copy(drafts = state.drafts + (idx to passenger), savedIndices = newSaved)
        }
    }

    fun selectSavedPassenger(idx: Int, passenger: Passenger) {
        _uiState.update {
            it.copy(
                drafts = it.drafts + (idx to passenger.copy(id = 0)),
                showSavedPickerForIdx = null
            )
        }
    }

    fun showSavedPicker(idx: Int) = _uiState.update { it.copy(showSavedPickerForIdx = idx) }
    fun hideSavedPicker() = _uiState.update { it.copy(showSavedPickerForIdx = null) }

    fun savePassengerToMyPassengers(idx: Int) {
        // Атомарно переводим в savingPassengerIdx, чтобы исключить гонку при двойном нажатии
        var draft: com.transport.tickets.domain.model.Passenger? = null
        _uiState.update { state ->
            if (idx in state.savedIndices || state.savingPassengerIdx == idx) return@update state
            val d = state.draftFor(idx)
            if (d.firstName.isBlank() || d.lastName.isBlank()) return@update state
            draft = d
            state.copy(savingPassengerIdx = idx)
        }
        val passengerToSave = draft ?: return
        viewModelScope.launch {
            try {
                savePassengerUseCase(passengerToSave)
                _uiState.update { it.copy(savingPassengerIdx = null, savedIndices = it.savedIndices + idx) }
            } catch (_: Exception) {
                _uiState.update { it.copy(savingPassengerIdx = null) }
            }
        }
    }

    fun confirmPassengers(): Boolean {
        val state = _uiState.value
        if (!state.allValid) return false
        state.seats.forEachIndexed { idx, seat ->
            purchaseFlowHolder.passengers[seat] = state.draftFor(idx)
        }
        return true
    }
}
