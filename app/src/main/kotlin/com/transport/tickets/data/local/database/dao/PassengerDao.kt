package com.transport.tickets.data.local.database.dao

import androidx.room.*
import com.transport.tickets.data.local.database.entities.PassengerEntity
import com.transport.tickets.data.local.database.entities.TicketPassengerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PassengerDao {
    @Query("SELECT * FROM passengers WHERE isSaved = 1 ORDER BY lastName, firstName")
    fun getAllPassengers(): Flow<List<PassengerEntity>>

    @Query("SELECT * FROM passengers WHERE id = :id")
    suspend fun getById(id: Int): PassengerEntity?

    @Query("""
        SELECT * FROM passengers
        WHERE isSaved = 1 AND documentType = :docType AND documentSeries = :series AND documentNumber = :number
        LIMIT 1
    """)
    suspend fun findByDocument(docType: String, series: String, number: String): PassengerEntity?

    @Upsert
    suspend fun upsert(passenger: PassengerEntity): Long

    @Delete
    suspend fun delete(passenger: PassengerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicketPassenger(tp: TicketPassengerEntity)

    @Query("DELETE FROM ticket_passengers WHERE ticketId = :ticketId")
    suspend fun deleteTicketPassengers(ticketId: Int)

    @Query("""
        SELECT p.* FROM passengers p
        INNER JOIN ticket_passengers tp ON p.id = tp.passengerId
        WHERE tp.ticketId = :ticketId
        ORDER BY tp.seatNumber
    """)
    suspend fun getPassengersForTicket(ticketId: Int): List<PassengerEntity>

    @Query("SELECT * FROM ticket_passengers WHERE ticketId = :ticketId ORDER BY seatNumber")
    suspend fun getTicketPassengers(ticketId: Int): List<TicketPassengerEntity>
}
