package com.transport.tickets

import com.transport.tickets.domain.model.Route
import com.transport.tickets.domain.model.Ticket
import com.transport.tickets.domain.repository.TicketRepository
import com.transport.tickets.domain.usecase.BuyTicketUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class BuyTicketUseCaseTest {

    private lateinit var repository: TicketRepository
    private lateinit var useCase: BuyTicketUseCase

    private val fakeTicket = Ticket(
        id = 1,
        userId = 42,
        routeId = 1,
        seatCount = 2,
        totalPrice = 3000.0,
        status = "active",
        createdAt = "2024-06-01T10:00:00Z",
        route = null
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = BuyTicketUseCase(repository)
    }

    @Test
    fun `buy ticket succeeds with valid parameters`() = runTest {
        coEvery { repository.buyTicket(1, 2) } returns fakeTicket

        val result = useCase(routeId = 1, seatCount = 2)

        assertEquals(fakeTicket, result)
        coVerify(exactly = 1) { repository.buyTicket(1, 2) }
    }

    @Test
    fun `buy ticket fails with zero seat count`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(routeId = 1, seatCount = 0) }
        }
    }

    @Test
    fun `buy ticket fails with more than 10 seats`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(routeId = 1, seatCount = 11) }
        }
    }

    @Test
    fun `buy ticket propagates repository exception`() = runTest {
        coEvery { repository.buyTicket(1, 1) } throws IllegalStateException("Not enough seats")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(routeId = 1, seatCount = 1) }
        }
    }

    @Test
    fun `buy ticket with max seats succeeds`() = runTest {
        coEvery { repository.buyTicket(1, 10) } returns fakeTicket.copy(seatCount = 10, totalPrice = 15000.0)

        val result = useCase(routeId = 1, seatCount = 10)

        assertEquals(10, result.seatCount)
    }
}
