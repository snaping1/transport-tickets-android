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

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[DARK_THEME] ?: false }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS] ?: true }
    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "ru" }
    val avatarUri: Flow<String?> = context.dataStore.data.map { it[AVATAR_URI] }

    suspend fun setDarkTheme(value: Boolean) = context.dataStore.edit { it[DARK_THEME] = value }
    suspend fun setNotifications(value: Boolean) = context.dataStore.edit { it[NOTIFICATIONS] = value }
    suspend fun setLanguage(value: String) = context.dataStore.edit { it[LANGUAGE] = value }
    suspend fun setAvatarUri(uri: String?) = context.dataStore.edit {
        if (uri != null) it[AVATAR_URI] = uri else it.remove(AVATAR_URI)
    }
}
