package com.transport.tickets.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val routeId: Int,
    val seatCount: Int,
    val totalPrice: Double,
    val status: String,
    val createdAt: String,
    val seatNumbers: String = "",
    // Denormalized route fields for offline access
    val routeOriginCity: String?,
    val routeDestinationCity: String?,
    val routeDepartureTime: String?,
    val routeArrivalTime: String?,
    val routePrice: Double?,
    val routeTransportType: String?
)
