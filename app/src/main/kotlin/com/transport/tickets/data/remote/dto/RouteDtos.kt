package com.transport.tickets.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteDto(
    val id: Int,
    @SerialName("originCity") val originCity: String,
    @SerialName("destinationCity") val destinationCity: String,
    @SerialName("departureTime") val departureTime: String,
    @SerialName("arrivalTime") val arrivalTime: String,
    val price: Double,
    @SerialName("totalSeats") val totalSeats: Int,
    @SerialName("availableSeats") val availableSeats: Int,
    @SerialName("transportType") val transportType: String,
    @SerialName("svAvailableSeats") val svAvailableSeats: Int? = null,
    @SerialName("coupeAvailableSeats") val coupeAvailableSeats: Int? = null,
    @SerialName("platzkartAvailableSeats") val platzkartAvailableSeats: Int? = null,
    @SerialName("seatCarAvailableSeats") val seatCarAvailableSeats: Int? = null
)

@Serializable
data class TicketDto(
    val id: Int,
    @SerialName("userId") val userId: Int,
    @SerialName("routeId") val routeId: Int,
    @SerialName("seatCount") val seatCount: Int,
    @SerialName("totalPrice") val totalPrice: Double,
    val status: String,
    @SerialName("createdAt") val createdAt: String,
    val route: RouteDto? = null,
    @SerialName("seatNumbers") val seatNumbers: List<Int> = emptyList()
)

@Serializable
data class VerifyTokenRequest(val idToken: String)

@Serializable
data class VerifyTokenResponse(
    val userId: Int,
    val firebaseUid: String,
    val email: String
)

@Serializable
data class BuyTicketRequest(
    val routeId: Int,
    val seatCount: Int,
    val seatNumbers: List<Int> = emptyList()
)
