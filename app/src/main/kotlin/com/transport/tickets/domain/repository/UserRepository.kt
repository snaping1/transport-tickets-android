package com.transport.tickets.domain.repository

import com.transport.tickets.domain.model.AppSettings
import com.transport.tickets.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getProfile(): Flow<UserProfile?>
    suspend fun saveProfile(profile: UserProfile)
    fun getSettings(): Flow<AppSettings>
    suspend fun setDarkTheme(value: Boolean)
    suspend fun setNotifications(value: Boolean)
    suspend fun setLanguage(value: String)
}
