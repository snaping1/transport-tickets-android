package com.transport.tickets.domain.model

data class Passenger(
    val id: Int = 0,
    val firstName: String = "",
    val lastName: String = "",
    val patronymic: String = "",
    val documentType: String = "passport",
    val documentSeries: String = "",
    val documentNumber: String = "",
    val birthDate: String = "",
    val gender: String = "male"
) {
    val fullName: String get() = listOf(lastName, firstName, patronymic).filter { it.isNotBlank() }.joinToString(" ")
    val shortName: String get() = buildString {
        append(lastName)
        if (firstName.isNotBlank()) append(" ${firstName.first()}.")
        if (patronymic.isNotBlank()) append("${patronymic.first()}.")
    }
    val documentLabel: String get() = when (documentType) {
        "foreign_passport" -> "Загранпаспорт"
        "birth_cert" -> "Свидетельство о рождении"
        else -> "Паспорт"
    }
    val documentFull: String get() = if (documentSeries.isNotBlank()) "$documentSeries $documentNumber" else documentNumber
}
