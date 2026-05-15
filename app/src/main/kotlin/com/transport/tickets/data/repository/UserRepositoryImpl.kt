package com.transport.tickets.data.repository

import com.google.firebase.auth.FirebaseAuth
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
    private val prefs: AppPreferences,
    private val firebaseAuth: FirebaseAuth
) : UserRepository {

    override fun getProfile(): Flow<UserProfile?> = dao.getProfile().map { entity ->
        val user = firebaseAuth.currentUser ?: return@map null
        UserProfile(
            uid = user.uid,
            email = user.email ?: "",
            displayName = entity?.displayName ?: user.displayName ?: "",
            phone = entity?.phone ?: "",
            birthDate = entity?.birthDate ?: "",
            registeredAt = user.metadata?.creationTimestamp ?: 0L
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
