package com.transport.tickets.domain.model

data class Ticket(
    val id: Int,
    val userId: Int,
    val routeId: Int,
    val seatCount: Int,
    val totalPrice: Double,
    val status: String,
    val createdAt: String,
    val route: Route?,
    val seatNumbers: List<Int> = emptyList()
)
