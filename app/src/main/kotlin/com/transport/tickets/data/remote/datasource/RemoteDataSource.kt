package com.transport.tickets.data.remote.datasource

import com.transport.tickets.data.remote.api.TransportApi
import com.transport.tickets.data.remote.dto.*
import javax.inject.Inject

class RemoteDataSource @Inject constructor(
    private val api: TransportApi
) {
    suspend fun register(email: String, password: String): AuthResponse =
        api.register(RegisterRequest(email, password))

    suspend fun login(email: String, password: String): AuthResponse =
        api.login(LoginRequest(email, password))

    suspend fun getRoutes(
        origin: String?,
        destination: String?,
        date: String?,
        transportType: String? = null
    ): List<RouteDto> = api.getRoutes(origin, destination, date, transportType)

    suspend fun getOccupiedSeats(routeId: Int): List<Int> =
        api.getOccupiedSeats(routeId)

    suspend fun buyTicket(routeId: Int, seatCount: Int, seatNumbers: List<Int>): TicketDto =
        api.buyTicket(BuyTicketRequest(routeId, seatCount, seatNumbers))

    suspend fun getMyTickets(): List<TicketDto> =
        api.getMyTickets()

    suspend fun cancelTicket(ticketId: Int) =
        api.cancelTicket(ticketId)
}
