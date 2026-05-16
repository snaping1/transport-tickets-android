package com.transport.tickets.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val zone = ZoneId.of("Europe/Moscow")
    private val dateFmt  = DateTimeFormatter.ofPattern("d MMMM, EEE", Locale("ru")).withZone(zone)
    private val timeFmt  = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
    private val shortFmt = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale("ru")).withZone(zone)

    fun formatDate(iso: String): String  = runCatching { dateFmt.format(Instant.parse(iso)) }.getOrDefault(iso)
    fun formatTime(iso: String): String  = runCatching { timeFmt.format(Instant.parse(iso)) }.getOrDefault(iso)
    fun formatShort(iso: String): String = runCatching { shortFmt.format(Instant.parse(iso)) }.getOrDefault(iso)

    fun durationMinutes(from: String, to: String): Long = runCatching {
        (Instant.parse(to).epochSecond - Instant.parse(from).epochSecond) / 60
    }.getOrDefault(0L)

    fun formatDuration(minutes: Long): String {
        val h = minutes / 60; val m = minutes % 60
        return if (h > 0) "${h}ч ${m}мин" else "${m}мин"
    }
}
