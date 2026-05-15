package com.transport.tickets.domain.usecase

import com.transport.tickets.domain.repository.TicketRepository
import javax.inject.Inject

class CancelTicketUseCase @Inject constructor(
    private val repository: TicketRepository
) {
    suspend operator fun invoke(ticketId: Int) = repository.cancelTicket(ticketId)
}
