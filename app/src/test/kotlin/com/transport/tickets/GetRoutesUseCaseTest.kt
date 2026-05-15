package com.transport.tickets

import com.transport.tickets.domain.model.Route
import com.transport.tickets.domain.repository.RouteRepository
import com.transport.tickets.domain.usecase.GetRoutesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetRoutesUseCaseTest {

    private lateinit var repository: RouteRepository
    private lateinit var useCase: GetRoutesUseCase

    private val fakeRoutes = listOf(
        Route(
            id = 1,
            originCity = "Москва",
            destinationCity = "Санкт-Петербург",
            departureTime = "2024-06-01T10:00:00Z",
            arrivalTime = "2024-06-01T14:00:00Z",
            price = 1500.0,
            totalSeats = 50,
            availableSeats = 30,
            transportType = "bus"
        ),
        Route(
            id = 2,
            originCity = "Москва",
            destinationCity = "Казань",
            departureTime = "2024-06-02T08:00:00Z",
            arrivalTime = "2024-06-02T16:00:00Z",
            price = 2200.0,
            totalSeats = 40,
            availableSeats = 20,
            transportType = "train"
        )
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetRoutesUseCase(repository)
    }

    @Test
    fun `invoke returns routes from repository`() = runTest {
        every { repository.getRoutes(null, null, null) } returns flowOf(fakeRoutes)

        val result = useCase().first()

        assertEquals(fakeRoutes, result)
    }

    @Test
    fun `invoke with filters passes them to repository`() = runTest {
        val filtered = listOf(fakeRoutes[0])
        every { repository.getRoutes("Москва", "Санкт-Петербург", null) } returns flowOf(filtered)

        val result = useCase(origin = "Москва", destination = "Санкт-Петербург").first()

        assertEquals(filtered, result)
        assertEquals(1, result.size)
    }

    @Test
    fun `refresh calls repository refresh`() = runTest {
        coEvery { repository.refreshRoutes(any(), any(), any()) } returns Unit

        useCase.refresh(origin = "Москва")

        coVerify { repository.refreshRoutes("Москва", null, null) }
    }

    @Test
    fun `invoke returns empty list when repository has no routes`() = runTest {
        every { repository.getRoutes(null, null, null) } returns flowOf(emptyList())

        val result = useCase().first()

        assertEquals(emptyList<Route>(), result)
    }
}
