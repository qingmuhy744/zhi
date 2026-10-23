package com.zhi.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.SecureRandom

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zhi_settings")

enum class ServerMode {
    DEFAULT,
    CUSTOM
}

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_DEVICE_KEY = stringPreferencesKey("device_key")
        val KEY_SERVER_MODE = stringPreferencesKey("server_mode")
        val KEY_CUSTOM_SERVER_URL = stringPreferencesKey("custom_server_url")
        val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
        val KEY_IS_REGISTERED = booleanPreferencesKey("is_registered")
        val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val KEY_AUTO_COPY = booleanPreferencesKey("auto_copy")
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")

        const val DEFAULT_SERVER_URL = "http://10.0.2.2:8080"
    }

    val deviceKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEVICE_KEY] ?: ""
    }

    val serverModeFlow: Flow<ServerMode> = context.dataStore.data.map { prefs ->
        val mode = prefs[KEY_SERVER_MODE] ?: ServerMode.DEFAULT.name
        try {
            ServerMode.valueOf(mode)
        } catch (e: Exception) {
            ServerMode.DEFAULT
        }
    }

    val customServerUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_SERVER_URL] ?: ""
    }

    val effectiveServerUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val mode = prefs[KEY_SERVER_MODE] ?: ServerMode.DEFAULT.name
        if (mode == ServerMode.CUSTOM.name) {
            val custom = prefs[KEY_CUSTOM_SERVER_URL]?.trim() ?: ""
            if (custom.isNotEmpty()) custom.removeSuffix("/") else DEFAULT_SERVER_URL
        } else {
            DEFAULT_SERVER_URL
        }
    }

    val fcmTokenFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_FCM_TOKEN] ?: ""
    }

    val isRegisteredFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_REGISTERED] ?: false
    }

    val lastSyncTimeFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_TIME] ?: 0L
    }

    val autoCopyFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_COPY] ?: true
    }

    val soundEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOUND_ENABLED] ?: true
    }

    val vibrationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_VIBRATION_ENABLED] ?: true
    }

    suspend fun getOrCreateDeviceKey(): String {
        val current = context.dataStore.data.first()[KEY_DEVICE_KEY]
        if (!current.isNullOrEmpty()) {
            return current
        }
        val newKey = generateDeviceKey()
        context.dataStore.edit { prefs ->
            prefs[KEY_DEVICE_KEY] = newKey
        }
        return newKey
    }

    suspend fun regenerateDeviceKey(): String {
        val newKey = generateDeviceKey()
        context.dataStore.edit { prefs ->
            prefs[KEY_DEVICE_KEY] = newKey
            prefs[KEY_IS_REGISTERED] = false
        }
        return newKey
    }

    suspend fun setServerMode(mode: ServerMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_MODE] = mode.name
            prefs[KEY_IS_REGISTERED] = false
        }
    }

    suspend fun setCustomServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_SERVER_URL] = url.trim().removeSuffix("/")
            prefs[KEY_IS_REGISTERED] = false
        }
    }

    suspend fun setFCMToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FCM_TOKEN] = token
        }
    }

    suspend fun setRegistered(registered: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_REGISTERED] = registered
            if (registered) {
                prefs[KEY_LAST_SYNC_TIME] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setAutoCopy(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_COPY] = enabled
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SOUND_ENABLED] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VIBRATION_ENABLED] = enabled
        }
    }

    private fun generateDeviceKey(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = SecureRandom()
        val sb = StringBuilder(22)
        repeat(22) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }
}
