package com.transport.tickets

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
        val seats = listOf(1, 2)
        coEvery { repository.buyTicket(1, 2, seats) } returns fakeTicket

        val result = useCase(routeId = 1, seatCount = 2, seatNumbers = seats)

        assertEquals(fakeTicket, result)
        coVerify(exactly = 1) { repository.buyTicket(1, 2, seats) }
    }

    @Test
    fun `buy ticket fails with zero seat count`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                useCase(routeId = 1, seatCount = 0, seatNumbers = emptyList())
            }
        }
    }

    @Test
    fun `buy ticket fails with more than 10 seats`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                useCase(routeId = 1, seatCount = 11, seatNumbers = (1..11).toList())
            }
        }
    }

    @Test
    fun `buy ticket propagates repository exception`() = runTest {
        val seats = listOf(1)
        coEvery { repository.buyTicket(1, 1, seats) } throws IllegalStateException("Not enough seats")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                useCase(routeId = 1, seatCount = 1, seatNumbers = seats)
            }
        }
    }

    @Test
    fun `buy ticket with max seats succeeds`() = runTest {
        val seats = (1..10).toList()
        coEvery { repository.buyTicket(1, 10, seats) } returns fakeTicket.copy(seatCount = 10, totalPrice = 15000.0)

        val result = useCase(routeId = 1, seatCount = 10, seatNumbers = seats)

        assertEquals(10, result.seatCount)
    }
}
