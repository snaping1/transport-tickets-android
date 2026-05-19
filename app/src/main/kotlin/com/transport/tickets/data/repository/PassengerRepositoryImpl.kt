package com.transport.tickets.data.repository

import com.transport.tickets.data.local.database.dao.PassengerDao
import com.transport.tickets.data.local.database.entities.PassengerEntity
import com.transport.tickets.data.local.database.entities.TicketPassengerEntity
import com.transport.tickets.domain.model.Passenger
import com.transport.tickets.domain.repository.PassengerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private fun PassengerEntity.toDomain() = Passenger(
    id = id, firstName = firstName, lastName = lastName, patronymic = patronymic,
    documentType = documentType, documentSeries = documentSeries, documentNumber = documentNumber,
    birthDate = birthDate, gender = gender
)

private fun Passenger.toEntity() = PassengerEntity(
    id = id, firstName = firstName, lastName = lastName, patronymic = patronymic,
    documentType = documentType, documentSeries = documentSeries, documentNumber = documentNumber,
    birthDate = birthDate, gender = gender
)

class PassengerRepositoryImpl @Inject constructor(
    private val dao: PassengerDao
) : PassengerRepository {

    override fun getAllPassengers(): Flow<List<Passenger>> =
        dao.getAllPassengers().map { list -> list.map { it.toDomain() } }

    override suspend fun savePassenger(passenger: Passenger): Int {
        val existing = dao.findByDocument(
            passenger.documentType,
            passenger.documentSeries,
            passenger.documentNumber
        )
        val entity = passenger.toEntity().copy(
            id = existing?.id ?: passenger.id,
            isSaved = true
        )
        return dao.upsert(entity).toInt()
    }

    override suspend fun deletePassenger(passenger: Passenger) =
        dao.delete(passenger.toEntity())

    override suspend fun getPassengersForTicket(ticketId: Int): List<Pair<Int, Passenger>> {
        val tps = dao.getTicketPassengers(ticketId)
        return tps.mapNotNull { tp ->
            val p = dao.getById(tp.passengerId)?.toDomain() ?: return@mapNotNull null
            Pair(tp.seatNumber, p)
        }
    }

    override suspend fun saveTicketPassengers(ticketId: Int, seatPassengers: Map<Int, Passenger>) {
        dao.deleteTicketPassengers(ticketId)
        seatPassengers.forEach { (seat, passenger) ->
            val savedId = if (passenger.id != 0) passenger.id
                          else dao.upsert(passenger.toEntity()).toInt()
            dao.insertTicketPassenger(TicketPassengerEntity(ticketId, seat, savedId))
        }
    }
}
