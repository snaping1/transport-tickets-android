package com.transport.tickets.domain.repository

import com.transport.tickets.domain.model.Route
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    fun getRoutes(origin: String?, destination: String?, transportType: String?): Flow<List<Route>>
    suspend fun refreshRoutes(origin: String?, destination: String?, date: String?, transportType: String?)
}
