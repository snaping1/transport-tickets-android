package com.transport.tickets.data.local.database.dao

import androidx.room.*
import com.transport.tickets.data.local.database.entities.TicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY createdAt DESC")
    fun getAllTickets(): Flow<List<TicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ticket: TicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tickets: List<TicketEntity>)

    @Query("DELETE FROM tickets")
    suspend fun deleteAll()

    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun findById(id: Int): TicketEntity?

    @Query("UPDATE tickets SET status = 'cancelled' WHERE id = :ticketId")
    suspend fun updateStatusCancelled(ticketId: Int)

    @Transaction
    suspend fun replaceAll(tickets: List<TicketEntity>) {
        deleteAll()
        insertAll(tickets)
    }
}
