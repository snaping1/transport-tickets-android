package com.transport.tickets.data.remote.api

import com.transport.tickets.data.remote.dto.*
import retrofit2.http.*

interface TransportApi {

    @POST("auth/verify")
    suspend fun verifyToken(@Body request: VerifyTokenRequest): VerifyTokenResponse

    @GET("routes")
    suspend fun getRoutes(
        @Query("origin") origin: String? = null,
        @Query("destination") destination: String? = null,
        @Query("date") date: String? = null,
        @Query("transportType") transportType: String? = null
    ): List<RouteDto>

    @GET("routes/{id}/seats")
    suspend fun getOccupiedSeats(@Path("id") routeId: Int): List<Int>

    @POST("tickets/buy")
    suspend fun buyTicket(@Body request: BuyTicketRequest): TicketDto

    @GET("tickets/my")
    suspend fun getMyTickets(): List<TicketDto>

    @DELETE("tickets/{id}")
    suspend fun cancelTicket(@Path("id") ticketId: Int)
}
