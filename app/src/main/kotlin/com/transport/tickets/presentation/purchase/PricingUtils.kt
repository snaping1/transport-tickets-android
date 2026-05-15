package com.transport.tickets.presentation.purchase

object PricingUtils {

    fun trainSeatMultiplier(seatNum: Int): Double = when {
        seatNum in 1..18 -> 2.5
        seatNum in 19..162 -> { val p = (seatNum - 19) % 4; if (p == 0 || p == 2) 2.0 else 1.6 }
        seatNum in 163..594 -> {
            val local = (seatNum - 163) % 54
            if (local < 36) when (local % 4) { 0 -> 1.3; 1 -> 0.85; 2 -> 1.1; else -> 0.80 }
            else if ((local - 36) % 2 == 0) 0.75 else 0.65
        }
        seatNum in 595..654 -> 0.7
        else -> 1.0
    }

    fun planeSeatMultiplier(seatNum: Int): Double {
        val row = (seatNum - 1) / 6 + 1
        val col = (seatNum - 1) % 6
        return if (row <= 2) 2.0
        else when (col) { 0, 5 -> 1.2; 1, 4 -> 0.9; else -> 1.05 }
    }

    fun busSeatMultiplier(seatNum: Int, totalSeats: Int): Double {
        val rowCount = (totalSeats + 3) / 4
        val row = (seatNum - 1) / 4 + 1
        val col = (seatNum - 1) % 4
        val base = if (col == 0 || col == 3) 1.1 else 0.95
        return if (row == rowCount) base * 0.85 else base
    }

    fun seatMultiplier(seatNum: Int, totalSeats: Int, transportType: String): Double = when (transportType) {
        "train" -> trainSeatMultiplier(seatNum)
        "plane" -> planeSeatMultiplier(seatNum)
        else -> busSeatMultiplier(seatNum, totalSeats)
    }

    fun priceRange(basePrice: Double, transportType: String): Pair<Int, Int> = when (transportType) {
        "train" -> (basePrice * 0.65).toInt() to (basePrice * 2.5).toInt()
        "plane" -> (basePrice * 0.9).toInt() to (basePrice * 2.0).toInt()
        else -> (basePrice * 0.81).toInt() to (basePrice * 1.1).toInt()
    }

    fun trainBerthLabel(seatNum: Int): String = when {
        seatNum in 1..18 -> "СВ"
        seatNum in 19..162 -> if ((seatNum - 19) % 4 in listOf(0, 2)) "Нижняя" else "Верхняя"
        seatNum in 163..594 -> {
            val local = (seatNum - 163) % 54
            if (local < 36) when (local % 4) { 0 -> "Нижн. окно"; 1 -> "Верхн. окно"; 2 -> "Нижн. пр."; else -> "Верхн. пр." }
            else if ((local - 36) % 2 == 0) "Бок. нижн." else "Бок. верхн."
        }
        seatNum in 595..654 -> "Сидячий"
        else -> ""
    }
}
