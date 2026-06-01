package com.transport.tickets

import com.transport.tickets.domain.model.Ticket
import com.transport.tickets.domain.repository.TicketRepository
import com.transport.tickets.domain.usecase.GetMyTicketsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetMyTicketsUseCaseTest {

    private lateinit var repository: TicketRepository
    private lateinit var useCase: GetMyTicketsUseCase

    private val fakeTickets = listOf(
        Ticket(id = 1, userId = 42, routeId = 10, seatCount = 1,
            totalPrice = 1500.0, status = "active", createdAt = "2024-06-01T10:00:00Z", route = null),
        Ticket(id = 2, userId = 42, routeId = 11, seatCount = 2,
            totalPrice = 3000.0, status = "cancelled", createdAt = "2024-06-02T12:00:00Z", route = null)
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetMyTicketsUseCase(repository)
    }

    @Test
    fun `invoke returns tickets from repository`() = runTest {
        every { repository.getMyTickets() } returns flowOf(fakeTickets)

        val result = useCase().first()

        assertEquals(fakeTickets, result)
        assertEquals(2, result.size)
    }

    @Test
    fun `invoke returns empty list when no tickets`() = runTest {
        every { repository.getMyTickets() } returns flowOf(emptyList())

        val result = useCase().first()

        assertEquals(emptyList<Ticket>(), result)
    }

    @Test
    fun `refresh calls repository refreshMyTickets`() = runTest {
        coEvery { repository.refreshMyTickets() } returns Unit

        useCase.refresh()

        coVerify(exactly = 1) { repository.refreshMyTickets() }
    }

    @Test
    fun `invoke returns only active tickets when filtered`() = runTest {
        val activeOnly = listOf(fakeTickets[0])
        every { repository.getMyTickets() } returns flowOf(activeOnly)

        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals("active", result[0].status)
    }
}
