package com.transport.tickets.domain.usecase

import com.transport.tickets.domain.model.Route
import com.transport.tickets.domain.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRoutesUseCase @Inject constructor(
    private val repository: RouteRepository
) {
    operator fun invoke(
        origin: String? = null,
        destination: String? = null,
        transportType: String? = null
    ): Flow<List<Route>> = repository.getRoutes(origin, destination, transportType)

    suspend fun refresh(
        origin: String? = null,
        destination: String? = null,
        date: String? = null,
        transportType: String? = null
    ) {
        repository.refreshRoutes(origin, destination, date, transportType)
    }
}
