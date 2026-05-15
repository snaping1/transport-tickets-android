package com.transport.tickets.domain.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val phone: String = "",
    val birthDate: String = "",
    val registeredAt: Long = 0L
)
