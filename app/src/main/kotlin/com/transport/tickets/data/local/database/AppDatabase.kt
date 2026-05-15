package com.transport.tickets.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.transport.tickets.data.local.database.dao.PassengerDao
import com.transport.tickets.data.local.database.dao.RouteDao
import com.transport.tickets.data.local.database.dao.TicketDao
import com.transport.tickets.data.local.database.dao.UserProfileDao
import com.transport.tickets.data.local.database.entities.PassengerEntity
import com.transport.tickets.data.local.database.entities.RouteEntity
import com.transport.tickets.data.local.database.entities.TicketEntity
import com.transport.tickets.data.local.database.entities.TicketPassengerEntity
import com.transport.tickets.data.local.database.entities.UserProfileEntity

@Database(
    entities = [
        RouteEntity::class,
        TicketEntity::class,
        UserProfileEntity::class,
        PassengerEntity::class,
        TicketPassengerEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun ticketDao(): TicketDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun passengerDao(): PassengerDao
}
