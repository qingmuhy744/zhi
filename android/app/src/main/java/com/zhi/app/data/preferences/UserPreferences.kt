package com.zhi.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.SecureRandom

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zhi_settings")

enum class ServerMode {
    DEFAULT,
    CUSTOM
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_DEVICE_KEY = stringPreferencesKey("device_key")
        val KEY_SERVER_MODE = stringPreferencesKey("server_mode")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_CUSTOM_SERVER_URL = stringPreferencesKey("custom_server_url")
        val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
        val KEY_IS_REGISTERED = booleanPreferencesKey("is_registered")
        val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val KEY_AUTO_COPY = booleanPreferencesKey("auto_copy")
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val KEY_E2EE_ENABLED = booleanPreferencesKey("e2ee_enabled")
        val KEY_AES_KEY = stringPreferencesKey("aes_key")
        val KEY_SUBSCRIBED_TOPICS = stringSetPreferencesKey("subscribed_topics")

        const val DEFAULT_SERVER_URL = "https://push.qingmuhy.com"
    }

    val subscribedTopicsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SUBSCRIBED_TOPICS] ?: emptySet()
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

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val mode = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(mode)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
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

    val e2eeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_E2EE_ENABLED] ?: false
    }

    val aesKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AES_KEY] ?: ""
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

    suspend fun setDeviceKey(key: String) {
        val clean = key.trim()
        if (clean.isNotEmpty()) {
            context.dataStore.edit { prefs ->
                prefs[KEY_DEVICE_KEY] = clean
                prefs[KEY_IS_REGISTERED] = false
            }
        }
    }

    suspend fun setServerMode(mode: ServerMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_MODE] = mode.name
            prefs[KEY_IS_REGISTERED] = false
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
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

    suspend fun setE2EEEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_E2EE_ENABLED] = enabled
        }
    }

    suspend fun setAesKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AES_KEY] = key.trim()
        }
    }

    suspend fun subscribeTopic(topic: String): Result<Unit> {
        val cleanTopic = topic.trim()
        if (cleanTopic.length < 8) {
            return Result.failure(IllegalArgumentException("频道密钥长度至少需要 8 位字符"))
        }
        return try {
            FirebaseMessaging.getInstance().subscribeToTopic(cleanTopic)
            context.dataStore.edit { prefs ->
                val current = prefs[KEY_SUBSCRIBED_TOPICS] ?: emptySet()
                prefs[KEY_SUBSCRIBED_TOPICS] = current + cleanTopic
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unsubscribeTopic(topic: String): Result<Unit> {
        val cleanTopic = topic.trim()
        return try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(cleanTopic)
            context.dataStore.edit { prefs ->
                val current = prefs[KEY_SUBSCRIBED_TOPICS] ?: emptySet()
                prefs[KEY_SUBSCRIBED_TOPICS] = current - cleanTopic
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateTopicSecret(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        val sb = StringBuilder("topic_")
        repeat(18) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
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
