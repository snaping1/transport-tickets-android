package com.transport.tickets

import com.transport.tickets.domain.model.Passenger
import com.transport.tickets.domain.repository.PassengerRepository
import com.transport.tickets.domain.usecase.DeletePassengerUseCase
import com.transport.tickets.domain.usecase.GetPassengersUseCase
import com.transport.tickets.domain.usecase.SavePassengerUseCase
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

class PassengerUseCaseTest {

    private lateinit var repository: PassengerRepository

    private val fakePassenger = Passenger(
        id = 1,
        firstName = "Иван",
        lastName = "Иванов",
        patronymic = "Иванович",
        documentType = "passport",
        documentSeries = "4520",
        documentNumber = "123456",
        birthDate = "1990-01-15",
        gender = "male"
    )

    @Before
    fun setUp() {
        repository = mockk()
    }

    @Test
    fun `GetPassengersUseCase returns list from repository`() = runTest {
        every { repository.getAllPassengers() } returns flowOf(listOf(fakePassenger))

        val result = GetPassengersUseCase(repository)().first()

        assertEquals(1, result.size)
        assertEquals("Иванов", result[0].lastName)
    }

    @Test
    fun `SavePassengerUseCase returns assigned id`() = runTest {
        coEvery { repository.savePassenger(fakePassenger) } returns 7

        val id = SavePassengerUseCase(repository)(fakePassenger)

        assertEquals(7, id)
        coVerify(exactly = 1) { repository.savePassenger(fakePassenger) }
    }

    @Test
    fun `DeletePassengerUseCase calls repository delete`() = runTest {
        coEvery { repository.deletePassenger(fakePassenger) } returns Unit

        DeletePassengerUseCase(repository)(fakePassenger)

        coVerify(exactly = 1) { repository.deletePassenger(fakePassenger) }
    }

    @Test
    fun `Passenger fullName is correctly assembled`() {
        assertEquals("Иванов Иван Иванович", fakePassenger.fullName)
    }

    @Test
    fun `Passenger documentLabel returns correct string for passport`() {
        assertEquals("Паспорт", fakePassenger.documentLabel)
        assertEquals("Загранпаспорт", fakePassenger.copy(documentType = "foreign_passport").documentLabel)
        assertEquals("Свидетельство о рождении", fakePassenger.copy(documentType = "birth_cert").documentLabel)
    }
}
