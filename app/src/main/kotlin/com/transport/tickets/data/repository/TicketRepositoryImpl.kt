package com.transport.tickets.data.repository

import com.transport.tickets.data.local.datasource.LocalDataSource
import com.transport.tickets.data.remote.datasource.RemoteDataSource
import com.transport.tickets.domain.model.Passenger
import com.transport.tickets.domain.model.Ticket
import com.transport.tickets.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TicketRepositoryImpl @Inject constructor(
    private val remoteDataSource: RemoteDataSource,
    private val localDataSource: LocalDataSource
) : TicketRepository {

    override fun getMyTickets(): Flow<List<Ticket>> =
        localDataSource.getAllTickets().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshMyTickets() {
        val dtos = remoteDataSource.getMyTickets()
        localDataSource.cacheTickets(dtos.map { it.toEntity() })
    }

    override suspend fun buyTicket(routeId: Int, seatCount: Int, seatNumbers: List<Int>, passengers: Map<Int, Passenger>): Ticket {
        val dto = remoteDataSource.buyTicket(routeId, seatCount, seatNumbers, passengers)
        localDataSource.insertTicket(dto.toEntity())
        return dto.toDomain()
    }

    override suspend fun cancelTicket(ticketId: Int) {
        remoteDataSource.cancelTicket(ticketId)
        localDataSource.markTicketCancelled(ticketId)
    }

    override suspend fun getOccupiedSeats(routeId: Int): List<Int> =
        remoteDataSource.getOccupiedSeats(routeId)
}
