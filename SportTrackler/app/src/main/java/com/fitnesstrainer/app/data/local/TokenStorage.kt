package com.fitnesstrainer.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenStorage(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN  = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID       = intPreferencesKey("user_id")
        private val KEY_USER_ROLE     = stringPreferencesKey("user_role")
        private val KEY_FIRST_NAME    = stringPreferencesKey("first_name")
        private val KEY_LAST_NAME     = stringPreferencesKey("last_name")
        private val KEY_EMAIL         = stringPreferencesKey("email")
        private val KEY_AVATAR_URL    = stringPreferencesKey("avatar_url")
    }

    suspend fun saveAuth(
        accessToken: String,
        refreshToken: String,
        userId: Int,
        role: String,
        firstName: String,
        lastName: String,
        email: String,
        avatarUrl: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN]  = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_USER_ID]       = userId
            prefs[KEY_USER_ROLE]     = role
            prefs[KEY_FIRST_NAME]    = firstName
            prefs[KEY_LAST_NAME]     = lastName
            prefs[KEY_EMAIL]         = email
            if (avatarUrl != null) prefs[KEY_AVATAR_URL] = avatarUrl
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun getAccessToken()  = context.dataStore.data.map { it[KEY_ACCESS_TOKEN]  }.first()
    suspend fun getRefreshToken() = context.dataStore.data.map { it[KEY_REFRESH_TOKEN] }.first()
    suspend fun getUserId()       = context.dataStore.data.map { it[KEY_USER_ID] ?: -1  }.first()
    suspend fun getUserRole()     = context.dataStore.data.map { it[KEY_USER_ROLE]      }.first()
    suspend fun getFirstName()    = context.dataStore.data.map { it[KEY_FIRST_NAME]     }.first()
    suspend fun getLastName()     = context.dataStore.data.map { it[KEY_LAST_NAME]      }.first()
    suspend fun getEmail()        = context.dataStore.data.map { it[KEY_EMAIL]          }.first()
    suspend fun getAvatarUrl()    = context.dataStore.data.map { it[KEY_AVATAR_URL]     }.first()
    suspend fun isLoggedIn()      = getAccessToken() != null
}
