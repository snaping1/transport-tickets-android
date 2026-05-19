package com.transport.tickets.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val DARK_THEME = booleanPreferencesKey("dark_theme")
    private val NOTIFICATIONS = booleanPreferencesKey("notifications")
    private val LANGUAGE = stringPreferencesKey("language")
    private val AVATAR_URI = stringPreferencesKey("avatar_uri")
    private val PASSPORT = stringPreferencesKey("user_passport")
    private val PAYMENT_CARDS = stringPreferencesKey("payment_cards")
    private val SEARCH_HISTORY = stringPreferencesKey("search_history")
    private val USER_TOKEN = stringPreferencesKey("user_token")
    private val USER_EMAIL = stringPreferencesKey("user_email")
    private val USER_ID = stringPreferencesKey("user_id")
    private val USER_CREATED_AT = stringPreferencesKey("user_created_at")

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[DARK_THEME] ?: false }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS] ?: true }
    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "ru" }
    val avatarUri: Flow<String?> = context.dataStore.data.map { it[AVATAR_URI] }
    val passport: Flow<String> = context.dataStore.data.map { it[PASSPORT] ?: "" }
    val paymentCards: Flow<String> = context.dataStore.data.map { it[PAYMENT_CARDS] ?: "" }
    val searchHistory: Flow<String> = context.dataStore.data.map { it[SEARCH_HISTORY] ?: "" }
    val userToken: Flow<String?> = context.dataStore.data.map { it[USER_TOKEN] }
    val userEmail: Flow<String> = context.dataStore.data.map { it[USER_EMAIL] ?: "" }
    val userId: Flow<Int> = context.dataStore.data.map { it[USER_ID]?.toIntOrNull() ?: 0 }
    val userCreatedAt: Flow<String> = context.dataStore.data.map { it[USER_CREATED_AT] ?: "" }

    suspend fun setDarkTheme(value: Boolean) = context.dataStore.edit { it[DARK_THEME] = value }
    suspend fun setNotifications(value: Boolean) = context.dataStore.edit { it[NOTIFICATIONS] = value }
    suspend fun setLanguage(value: String) = context.dataStore.edit { it[LANGUAGE] = value }
    suspend fun setAvatarUri(uri: String?) = context.dataStore.edit {
        if (uri != null) it[AVATAR_URI] = uri else it.remove(AVATAR_URI)
    }
    suspend fun setPassport(v: String) = context.dataStore.edit { it[PASSPORT] = v }
    suspend fun setPaymentCards(v: String) = context.dataStore.edit { it[PAYMENT_CARDS] = v }
    suspend fun setSearchHistory(v: String) = context.dataStore.edit { it[SEARCH_HISTORY] = v }

    suspend fun saveAuth(token: String, userId: Int, email: String, createdAt: String) =
        context.dataStore.edit {
            it[USER_TOKEN] = token
            it[USER_ID] = userId.toString()
            it[USER_EMAIL] = email
            it[USER_CREATED_AT] = createdAt
        }

    suspend fun clearAuth() = context.dataStore.edit {
        it.remove(USER_TOKEN)
        it.remove(USER_ID)
        it.remove(USER_EMAIL)
        it.remove(USER_CREATED_AT)
    }
}
