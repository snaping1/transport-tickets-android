package com.transport.tickets.domain.repository

import com.transport.tickets.domain.model.Passenger
import kotlinx.coroutines.flow.Flow

interface PassengerRepository {
    fun getAllPassengers(): Flow<List<Passenger>>
    suspend fun savePassenger(passenger: Passenger): Int
    suspend fun deletePassenger(passenger: Passenger)
    suspend fun getPassengersForTicket(ticketId: Int): List<Pair<Int, Passenger>>
    suspend fun saveTicketPassengers(ticketId: Int, seatPassengers: Map<Int, Passenger>)
}
