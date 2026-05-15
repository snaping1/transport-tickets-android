package com.transport.tickets.data.local.datasource

import com.transport.tickets.data.local.database.dao.RouteDao
import com.transport.tickets.data.local.database.dao.TicketDao
import com.transport.tickets.data.local.database.entities.RouteEntity
import com.transport.tickets.data.local.database.entities.TicketEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalDataSource @Inject constructor(
    private val routeDao: RouteDao,
    private val ticketDao: TicketDao
) {
    fun getAllRoutes(): Flow<List<RouteEntity>> = routeDao.getAllRoutes()

    fun getFilteredRoutes(origin: String?, destination: String?): Flow<List<RouteEntity>> =
        routeDao.getFilteredRoutes(origin, destination)

    suspend fun cacheRoutes(routes: List<RouteEntity>) = routeDao.replaceAll(routes)

    fun getAllTickets(): Flow<List<TicketEntity>> = ticketDao.getAllTickets()

    suspend fun cacheTickets(tickets: List<TicketEntity>) = ticketDao.replaceAll(tickets)

    suspend fun insertTicket(ticket: TicketEntity) = ticketDao.insert(ticket)

    suspend fun markTicketCancelled(ticketId: Int) = ticketDao.updateStatusCancelled(ticketId)

    suspend fun clearAllCache() {
        routeDao.deleteAll()
        ticketDao.deleteAll()
    }
}
