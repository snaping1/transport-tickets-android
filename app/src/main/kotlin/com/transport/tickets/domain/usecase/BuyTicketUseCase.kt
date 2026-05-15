package com.transport.tickets.domain.usecase

import com.transport.tickets.domain.model.Ticket
import com.transport.tickets.domain.repository.TicketRepository
import javax.inject.Inject

class BuyTicketUseCase @Inject constructor(
    private val repository: TicketRepository
) {
    suspend operator fun invoke(routeId: Int, seatCount: Int, seatNumbers: List<Int>): Ticket {
        require(seatCount in 1..10) { "Seat count must be between 1 and 10" }
        return repository.buyTicket(routeId, seatCount, seatNumbers)
    }

    suspend fun getOccupiedSeats(routeId: Int): List<Int> =
        repository.getOccupiedSeats(routeId)
}
