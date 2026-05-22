package com.transport.tickets.util

object TransportIcons {
    fun emoji(type: String): String = when (type) {
        "bus"   -> "🚌"
        "train" -> "🚂"
        "plane" -> "✈️"
        else    -> "🚗"
    }

    fun label(type: String): String = when (type) {
        "bus"   -> "Автобус"
        "train" -> "Поезд"
        "plane" -> "Самолёт"
        else    -> type.replaceFirstChar { it.uppercase() }
    }

    fun description(type: String): String = when (type) {
        "bus"   -> "Комфортный автобус"
        "train" -> "Железнодорожный транспорт"
        "plane" -> "Авиаперелёт"
        else    -> type
    }
}
