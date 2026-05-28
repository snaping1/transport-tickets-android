package com.transport.tickets.domain.repository

import com.transport.tickets.domain.model.Passenger
import com.transport.tickets.domain.model.Ticket
import kotlinx.coroutines.flow.Flow

interface TicketRepository {
    fun getMyTickets(): Flow<List<Ticket>>
    suspend fun refreshMyTickets()
    suspend fun buyTicket(routeId: Int, seatCount: Int, seatNumbers: List<Int>, passengers: Map<Int, Passenger> = emptyMap()): Ticket
    suspend fun cancelTicket(ticketId: Int)
    suspend fun getOccupiedSeats(routeId: Int): List<Int>
}
