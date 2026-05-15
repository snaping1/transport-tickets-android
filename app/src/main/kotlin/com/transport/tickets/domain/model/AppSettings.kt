package com.transport.tickets.domain.model

data class AppSettings(
    val isDarkTheme: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val language: String = "ru"
)
