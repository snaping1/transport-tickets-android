package com.transport.tickets.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passengers")
data class PassengerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firstName: String = "",
    val lastName: String = "",
    val patronymic: String = "",
    val documentType: String = "passport",
    val documentSeries: String = "",
    val documentNumber: String = "",
    val birthDate: String = "",
    val gender: String = "male",
    val isSaved: Boolean = false
)

@Entity(tableName = "ticket_passengers", primaryKeys = ["ticketId", "seatNumber"])
data class TicketPassengerEntity(
    val ticketId: Int,
    val seatNumber: Int,
    val passengerId: Int
)
