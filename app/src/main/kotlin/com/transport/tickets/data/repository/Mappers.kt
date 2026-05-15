package com.transport.tickets.data.repository

import com.transport.tickets.data.local.database.entities.RouteEntity
import com.transport.tickets.data.local.database.entities.TicketEntity
import com.transport.tickets.data.remote.dto.RouteDto
import com.transport.tickets.data.remote.dto.TicketDto
import com.transport.tickets.domain.model.Route
import com.transport.tickets.domain.model.Ticket

fun RouteDto.toDomain() = Route(
    id = id,
    originCity = originCity,
    destinationCity = destinationCity,
    departureTime = departureTime,
    arrivalTime = arrivalTime,
    price = price,
    totalSeats = totalSeats,
    availableSeats = availableSeats,
    transportType = transportType,
    svAvailableSeats = svAvailableSeats,
    coupeAvailableSeats = coupeAvailableSeats,
    platzkartAvailableSeats = platzkartAvailableSeats,
    seatCarAvailableSeats = seatCarAvailableSeats
)

fun RouteDto.toEntity() = RouteEntity(
    id = id,
    originCity = originCity,
    destinationCity = destinationCity,
    departureTime = departureTime,
    arrivalTime = arrivalTime,
    price = price,
    totalSeats = totalSeats,
    availableSeats = availableSeats,
    transportType = transportType
)

fun RouteEntity.toDomain() = Route(
    id = id,
    originCity = originCity,
    destinationCity = destinationCity,
    departureTime = departureTime,
    arrivalTime = arrivalTime,
    price = price,
    totalSeats = totalSeats,
    availableSeats = availableSeats,
    transportType = transportType
)

fun TicketDto.toDomain() = Ticket(
    id = id,
    userId = userId,
    routeId = routeId,
    seatCount = seatCount,
    totalPrice = totalPrice,
    status = status,
    createdAt = createdAt,
    route = route?.toDomain(),
    seatNumbers = seatNumbers
)

fun TicketDto.toEntity() = TicketEntity(
    id = id,
    userId = userId,
    routeId = routeId,
    seatCount = seatCount,
    totalPrice = totalPrice,
    status = status,
    createdAt = createdAt,
    seatNumbers = seatNumbers.joinToString(","),
    routeOriginCity = route?.originCity,
    routeDestinationCity = route?.destinationCity,
    routeDepartureTime = route?.departureTime,
    routeArrivalTime = route?.arrivalTime,
    routePrice = route?.price,
    routeTransportType = route?.transportType
)

fun TicketEntity.toDomain(): Ticket {
    val route = if (routeOriginCity != null) {
        Route(
            id = routeId,
            originCity = routeOriginCity,
            destinationCity = routeDestinationCity ?: "",
            departureTime = routeDepartureTime ?: "",
            arrivalTime = routeArrivalTime ?: "",
            price = routePrice ?: 0.0,
            totalSeats = 0,
            availableSeats = 0,
            transportType = routeTransportType ?: ""
        )
    } else null
    return Ticket(
        id = id,
        userId = userId,
        routeId = routeId,
        seatCount = seatCount,
        totalPrice = totalPrice,
        status = status,
        createdAt = createdAt,
        route = route,
        seatNumbers = seatNumbers.split(",").mapNotNull { it.trim().toIntOrNull() }
    )
}
