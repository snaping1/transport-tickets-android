package com.transport.tickets.domain.usecase

import com.transport.tickets.domain.model.Passenger
import com.transport.tickets.domain.repository.PassengerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPassengersUseCase @Inject constructor(private val repo: PassengerRepository) {
    operator fun invoke(): Flow<List<Passenger>> = repo.getAllPassengers()
}

class SavePassengerUseCase @Inject constructor(private val repo: PassengerRepository) {
    suspend operator fun invoke(passenger: Passenger): Int = repo.savePassenger(passenger)
}

class DeletePassengerUseCase @Inject constructor(private val repo: PassengerRepository) {
    suspend operator fun invoke(passenger: Passenger) = repo.deletePassenger(passenger)
}

class GetTicketPassengersUseCase @Inject constructor(private val repo: PassengerRepository) {
    suspend operator fun invoke(ticketId: Int): List<Pair<Int, Passenger>> =
        repo.getPassengersForTicket(ticketId)
}

class SaveTicketPassengersUseCase @Inject constructor(private val repo: PassengerRepository) {
    suspend operator fun invoke(ticketId: Int, seatPassengers: Map<Int, Passenger>) =
        repo.saveTicketPassengers(ticketId, seatPassengers)
}
