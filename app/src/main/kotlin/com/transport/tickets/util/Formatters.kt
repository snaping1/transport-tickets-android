package com.transport.tickets.util

object Formatters {
    fun transportLabel(type: String): String = when (type) {
        "bus"   -> "Автобус"
        "train" -> "Поезд"
        "plane" -> "Самолёт"
        else    -> type
    }

    fun seatLabel(seat: Int, transportType: String): String {
        if (transportType != "train") return "Место $seat"
        return when (seat) {
            in 1..18    -> "СВ, место $seat"
            in 19..162  -> "Купе, место $seat"
            in 163..594 -> "Плацкарт, место $seat"
            in 595..654 -> "Сидячий, место $seat"
            else        -> "Место $seat"
        }
    }

    fun statusLabel(status: String): String = when (status) {
        "active"    -> "Активен"
        "cancelled" -> "Отменён"
        else        -> status
    }

    fun genderLabel(gender: String): String = when (gender) {
        "male"   -> "Мужской"
        "female" -> "Женский"
        else     -> gender
    }
}
