package com.transport.tickets.data.remote.datasource

import com.transport.tickets.data.remote.api.TransportApi
import com.transport.tickets.data.remote.dto.*
import com.transport.tickets.domain.model.Passenger
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

    suspend fun buyTicket(routeId: Int, seatCount: Int, seatNumbers: List<Int>, passengers: Map<Int, Passenger> = emptyMap()): TicketDto {
        val passengerDtos = passengers.map { (seatNum, p) ->
            PassengerData(
                seatNumber     = seatNum,
                firstName      = p.firstName,
                lastName       = p.lastName,
                patronymic     = p.patronymic,
                documentType   = p.documentType,
                documentSeries = p.documentSeries,
                documentNumber = p.documentNumber,
                birthDate      = p.birthDate.digitsToIsoDate(),
                gender         = p.gender
            )
        }
        return api.buyTicket(BuyTicketRequest(routeId, seatCount, seatNumbers, passengerDtos))
    }

    // Converts raw digit string "ddmmyyyy" (8 digits) → "yyyy-mm-dd" for PostgreSQL DATE.
    // Returns empty string for anything that doesn't match the expected 8-digit format.
    private fun String.digitsToIsoDate(): String {
        val digits = filter(Char::isDigit)
        if (digits.length < 8) return ""
        return "${digits.substring(4, 8)}-${digits.substring(2, 4)}-${digits.substring(0, 2)}"
    }

    suspend fun getMyTickets(): List<TicketDto> =
        api.getMyTickets()

    suspend fun cancelTicket(ticketId: Int) =
        api.cancelTicket(ticketId)
}
