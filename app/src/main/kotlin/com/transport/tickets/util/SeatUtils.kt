package com.transport.tickets.util

object SeatUtils {
    enum class WagonType { SV, COUPE, PLATZKART, SEAT_CAR, STANDARD }

    fun wagonType(seat: Int): WagonType = when (seat) {
        in 1..18    -> WagonType.SV
        in 19..162  -> WagonType.COUPE
        in 163..594 -> WagonType.PLATZKART
        in 595..654 -> WagonType.SEAT_CAR
        else        -> WagonType.STANDARD
    }

    fun wagonLabel(seat: Int, transportType: String): String {
        if (transportType != "train") return "Место $seat"
        return when (wagonType(seat)) {
            WagonType.SV        -> "СВ"
            WagonType.COUPE     -> "Купе"
            WagonType.PLATZKART -> "Плацкарт"
            WagonType.SEAT_CAR  -> "Сидячий"
            WagonType.STANDARD  -> "Место"
        }
    }

    fun isUpperBerth(seat: Int): Boolean {
        if (seat !in 19..594) return false
        val local = if (seat <= 162) (seat - 19) % 4 else (seat - 163) % 4
        return local == 1 || local == 3
    }
}
