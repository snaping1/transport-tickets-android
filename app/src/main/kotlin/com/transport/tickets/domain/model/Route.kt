package com.transport.tickets.domain.model

data class Route(
    val id: Int,
    val originCity: String,
    val destinationCity: String,
    val departureTime: String,
    val arrivalTime: String,
    val price: Double,
    val totalSeats: Int,
    val availableSeats: Int,
    val transportType: String,
    val svAvailableSeats: Int? = null,
    val coupeAvailableSeats: Int? = null,
    val platzkartAvailableSeats: Int? = null,
    val seatCarAvailableSeats: Int? = null
)
