package com.transport.tickets.util

import androidx.compose.ui.graphics.Color

object ThemeUtils {
    fun statusColor(status: String): Color = when (status) {
        "active"    -> Color(0xFF4CAF50)
        "cancelled" -> Color(0xFFF44336)
        "used"      -> Color(0xFF9E9E9E)
        else        -> Color(0xFF2196F3)
    }

    fun transportColor(type: String): Color = when (type) {
        "bus"   -> Color(0xFF4CAF50)
        "train" -> Color(0xFF2196F3)
        "plane" -> Color(0xFF9C27B0)
        else    -> Color(0xFF607D8B)
    }

    fun wagonColor(wagonType: SeatUtils.WagonType): Color = when (wagonType) {
        SeatUtils.WagonType.SV        -> Color(0xFFFF9800)
        SeatUtils.WagonType.COUPE     -> Color(0xFF2196F3)
        SeatUtils.WagonType.PLATZKART -> Color(0xFF4CAF50)
        SeatUtils.WagonType.SEAT_CAR  -> Color(0xFF9C27B0)
        SeatUtils.WagonType.STANDARD  -> Color(0xFF607D8B)
    }
}
