package com.transport.tickets.presentation.purchase

import com.transport.tickets.domain.model.Passenger
import com.transport.tickets.domain.model.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseFlowHolder @Inject constructor() {
    var route: Route? = null
    var selectedSeats: List<Int> = emptyList()
    val passengers: MutableMap<Int, Passenger> = mutableMapOf() // seatNumber -> Passenger

    fun totalPrice(): Double {
        val r = route ?: return 0.0
        return selectedSeats.sumOf { seat ->
            r.price * PricingUtils.seatMultiplier(seat, r.totalSeats, r.transportType)
        }
    }

    fun clear() {
        route = null
        selectedSeats = emptyList()
        passengers.clear()
    }
}
