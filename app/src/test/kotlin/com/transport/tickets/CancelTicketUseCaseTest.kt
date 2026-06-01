package com.transport.tickets

import com.transport.tickets.domain.repository.TicketRepository
import com.transport.tickets.domain.usecase.CancelTicketUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CancelTicketUseCaseTest {

    private lateinit var repository: TicketRepository
    private lateinit var useCase: CancelTicketUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = CancelTicketUseCase(repository)
    }

    @Test
    fun `cancel ticket calls repository with correct id`() = runTest {
        coEvery { repository.cancelTicket(5) } returns Unit

        useCase(5)

        coVerify(exactly = 1) { repository.cancelTicket(5) }
    }

    @Test
    fun `cancel ticket propagates repository exception`() = runTest {
        coEvery { repository.cancelTicket(99) } throws IllegalStateException("Ticket not found")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(99) }
        }
    }

}
