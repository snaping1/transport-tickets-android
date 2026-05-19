package com.transport.tickets.data.repository

import com.transport.tickets.data.local.database.dao.UserProfileDao
import com.transport.tickets.data.local.database.entities.UserProfileEntity
import com.transport.tickets.data.preferences.AppPreferences
import com.transport.tickets.domain.model.AppSettings
import com.transport.tickets.domain.model.UserProfile
import com.transport.tickets.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val dao: UserProfileDao,
    private val prefs: AppPreferences
) : UserRepository {

    override fun getProfile(): Flow<UserProfile?> = combine(
        prefs.userEmail,
        prefs.userId,
        prefs.userCreatedAt,
        dao.getProfile()
    ) { email, userId, createdAt, entity ->
        if (email.isEmpty()) return@combine null
        UserProfile(
            uid = userId.toString(),
            email = email,
            displayName = entity?.displayName ?: "",
            phone = entity?.phone ?: "",
            birthDate = entity?.birthDate ?: "",
            registeredAt = runCatching {
                java.time.Instant.parse(createdAt).toEpochMilli()
            }.getOrDefault(0L)
        )
    }

    override suspend fun saveProfile(profile: UserProfile) {
        dao.upsert(UserProfileEntity(
            uid = profile.uid,
            displayName = profile.displayName,
            phone = profile.phone,
            birthDate = profile.birthDate
        ))
    }

    override fun getSettings(): Flow<AppSettings> = combine(
        prefs.isDarkTheme,
        prefs.notificationsEnabled,
        prefs.language
    ) { dark, notifs, lang -> AppSettings(dark, notifs, lang) }

    override suspend fun setDarkTheme(value: Boolean) { prefs.setDarkTheme(value) }
    override suspend fun setNotifications(value: Boolean) { prefs.setNotifications(value) }
    override suspend fun setLanguage(value: String) { prefs.setLanguage(value) }
}
