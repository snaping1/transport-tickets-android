package com.transport.tickets.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminLoginRequest(val email: String, val password: String)

@Serializable
data class AdminLoginResponse(val token: String, val email: String)

@Serializable
data class CreateRouteRequest(
    val originCity: String,
    val destinationCity: String,
    val departureTime: String,
    val arrivalTime: String,
    val price: Double,
    val totalSeats: Int,
    val transportType: String
)
