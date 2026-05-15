package com.transport.tickets.domain.usecase

import com.transport.tickets.domain.model.Ticket
import com.transport.tickets.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMyTicketsUseCase @Inject constructor(
    private val repository: TicketRepository
) {
    operator fun invoke(): Flow<List<Ticket>> = repository.getMyTickets()

    suspend fun refresh() = repository.refreshMyTickets()
}
