package com.transport.tickets.data.repository

import com.transport.tickets.data.local.datasource.LocalDataSource
import com.transport.tickets.data.remote.datasource.RemoteDataSource
import com.transport.tickets.domain.model.Route
import com.transport.tickets.domain.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor(
    private val remoteDataSource: RemoteDataSource,
    private val localDataSource: LocalDataSource
) : RouteRepository {

    override fun getRoutes(origin: String?, destination: String?, transportType: String?): Flow<List<Route>> =
        localDataSource.getFilteredRoutes(origin, destination).map { entities ->
            entities.map { it.toDomain() }
                .let { routes ->
                    if (transportType.isNullOrBlank()) routes
                    else routes.filter { it.transportType == transportType }
                }
        }

    override suspend fun refreshRoutes(origin: String?, destination: String?, date: String?, transportType: String?) {
        val dtos = remoteDataSource.getRoutes(origin, destination, date, transportType)
        localDataSource.cacheRoutes(dtos.map { it.toEntity() })
    }
}
