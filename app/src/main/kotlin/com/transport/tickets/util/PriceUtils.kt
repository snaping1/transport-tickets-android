package com.transport.tickets.util

import java.text.NumberFormat
import java.util.Locale

object PriceUtils {
    private val fmt = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
        maximumFractionDigits = 0
    }

    fun format(price: Double): String = "${fmt.format(price)} ₽"

    fun formatWithLabel(price: Double, label: String = "Итого"): String =
        "$label: ${format(price)}"

    fun formatDiff(original: Double, discounted: Double): String {
        val saved = original - discounted
        return if (saved > 0) "Скидка ${format(saved)}" else format(discounted)
    }
}
