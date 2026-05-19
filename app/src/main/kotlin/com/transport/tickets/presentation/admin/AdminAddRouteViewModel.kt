package com.transport.tickets.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transport.tickets.data.remote.dto.CreateRouteRequest
import com.transport.tickets.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AdminAddRouteState {
    data object Idle : AdminAddRouteState
    data object Loading : AdminAddRouteState
    data object Success : AdminAddRouteState
    data class Error(val message: String) : AdminAddRouteState
}

@HiltViewModel
class AdminAddRouteViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AdminAddRouteState>(AdminAddRouteState.Idle)
    val state: StateFlow<AdminAddRouteState> = _state.asStateFlow()

    private val _cities = MutableStateFlow<List<String>>(emptyList())
    val cities: StateFlow<List<String>> = _cities.asStateFlow()

    init {
        loadCities()
    }

    private fun loadCities() {
        viewModelScope.launch {
            runCatching {
                val routes = adminRepository.getAllRoutes()
                (routes.map { it.originCity } + routes.map { it.destinationCity })
                    .distinct()
                    .sorted()
            }.onSuccess { _cities.value = it }
        }
    }

    fun createRoute(
        originCity: String,
        destinationCity: String,
        departureDate: String,
        departureTime: String,
        arrivalDate: String,
        arrivalTime: String,
        price: String,
        totalSeats: String,
        transportType: String
    ) {
        viewModelScope.launch {
            _state.value = AdminAddRouteState.Loading
            try {
                if (originCity.isBlank()) throw IllegalArgumentException("Укажите город отправления")
                if (destinationCity.isBlank()) throw IllegalArgumentException("Укажите город назначения")
                if (departureDate.isBlank() || departureTime.isBlank()) throw IllegalArgumentException("Укажите дату и время отправления")
                if (arrivalDate.isBlank() || arrivalTime.isBlank()) throw IllegalArgumentException("Укажите дату и время прибытия")
                val priceVal = price.toDoubleOrNull()?.takeIf { it > 0 }
                    ?: throw IllegalArgumentException("Введите корректную цену")
                val seatsVal = totalSeats.toIntOrNull()?.takeIf { it > 0 }
                    ?: throw IllegalArgumentException("Введите корректное количество мест")

                adminRepository.createRoute(
                    CreateRouteRequest(
                        originCity = originCity.trim(),
                        destinationCity = destinationCity.trim(),
                        departureTime = "${departureDate}T${departureTime}:00Z",
                        arrivalTime = "${arrivalDate}T${arrivalTime}:00Z",
                        price = priceVal,
                        totalSeats = seatsVal,
                        transportType = transportType
                    )
                )
                _state.value = AdminAddRouteState.Success
            } catch (e: Exception) {
                _state.value = AdminAddRouteState.Error(e.message ?: "Ошибка при создании рейса")
            }
        }
    }

    fun resetState() { _state.value = AdminAddRouteState.Idle }
}
