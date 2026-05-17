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
        private val KEY_ACCESS_TOKEN       = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN      = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID            = intPreferencesKey("user_id")
        private val KEY_USER_ROLE          = stringPreferencesKey("user_role")
        private val KEY_FIRST_NAME         = stringPreferencesKey("first_name")
        private val KEY_LAST_NAME          = stringPreferencesKey("last_name")
        private val KEY_EMAIL              = stringPreferencesKey("email")
        private val KEY_AVATAR_URL         = stringPreferencesKey("avatar_url")
        private val KEY_BIOMETRIC_ENABLED  = booleanPreferencesKey("biometric_enabled")
        private val KEY_REMEMBER_ME        = booleanPreferencesKey("remember_me")
        private val KEY_SAVED_EMAIL        = stringPreferencesKey("saved_email")
        private val KEY_WORKOUT_REMINDER   = booleanPreferencesKey("workout_reminder")
        private val KEY_REMINDER_HOUR      = intPreferencesKey("reminder_hour")
        private val KEY_REMINDER_MINUTE    = intPreferencesKey("reminder_minute")
        private val KEY_PIN_HASH           = stringPreferencesKey("pin_hash")
        private val KEY_PIN_OFFERED        = booleanPreferencesKey("pin_offered")
        private val KEY_PHONE              = stringPreferencesKey("phone")
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

    suspend fun saveAvatarUrl(url: String) {
        context.dataStore.edit { it[KEY_AVATAR_URL] = url }
    }

    suspend fun saveProfile(firstName: String, lastName: String, phone: String?) {
        context.dataStore.edit {
            it[KEY_FIRST_NAME] = firstName
            it[KEY_LAST_NAME]  = lastName
            if (phone != null) it[KEY_PHONE] = phone else it.remove(KEY_PHONE)
        }
    }

    suspend fun getPhone() = context.dataStore.data.map { it[KEY_PHONE] }.first()

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
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

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }
    suspend fun isBiometricEnabled() =
        context.dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }.first()

    suspend fun setRememberMe(enabled: Boolean, email: String = "") {
        context.dataStore.edit {
            it[KEY_REMEMBER_ME]  = enabled
            if (enabled) it[KEY_SAVED_EMAIL] = email else it.remove(KEY_SAVED_EMAIL)
        }
    }
    suspend fun isRememberMe()  = context.dataStore.data.map { it[KEY_REMEMBER_ME]  ?: false }.first()
    suspend fun getSavedEmail() = context.dataStore.data.map { it[KEY_SAVED_EMAIL]  }.first()

    suspend fun setWorkoutReminder(enabled: Boolean, hour: Int = 8, minute: Int = 0) {
        context.dataStore.edit {
            it[KEY_WORKOUT_REMINDER] = enabled
            it[KEY_REMINDER_HOUR]    = hour
            it[KEY_REMINDER_MINUTE]  = minute
        }
    }
    suspend fun setPinOffered(offered: Boolean) {
        context.dataStore.edit { it[KEY_PIN_OFFERED] = offered }
    }
    suspend fun isPinOffered() = context.dataStore.data.map { it[KEY_PIN_OFFERED] ?: false }.first()

    suspend fun savePinHash(hash: String) {
        context.dataStore.edit { it[KEY_PIN_HASH] = hash }
    }
    suspend fun getPinHash() = context.dataStore.data.map { it[KEY_PIN_HASH] }.first()
    suspend fun hasPinSet()  = getPinHash() != null
    suspend fun clearPin()   = context.dataStore.edit { it.remove(KEY_PIN_HASH) }

    suspend fun isWorkoutReminderEnabled() =
        context.dataStore.data.map { it[KEY_WORKOUT_REMINDER] ?: false }.first()
    suspend fun getReminderHour()   = context.dataStore.data.map { it[KEY_REMINDER_HOUR]   ?: 8 }.first()
    suspend fun getReminderMinute() = context.dataStore.data.map { it[KEY_REMINDER_MINUTE] ?: 0 }.first()
}
