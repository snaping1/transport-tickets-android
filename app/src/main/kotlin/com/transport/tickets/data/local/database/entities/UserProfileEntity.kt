package com.transport.tickets.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val displayName: String = "",
    val phone: String = "",
    val birthDate: String = ""
)
