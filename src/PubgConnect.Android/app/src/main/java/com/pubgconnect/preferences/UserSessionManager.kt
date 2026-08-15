package com.pubgconnect.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.pubgconnect.models.UserDto

class UserSessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pubg_connect_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_JSON = "user_json"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_NOTIF_ENABLED = "notif_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATE_ENABLED = "vibrate_enabled"
        private const val KEY_SIMULATION_ENABLED = "simulation_enabled"
        private const val DEFAULT_SERVER_URL = "http://84.235.248.234:5000"
    }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    val isLoggedIn: Boolean
        get() = !token.isNullOrBlank()

    var currentUser: UserDto?
        get() {
            val json = prefs.getString(KEY_USER_JSON, null) ?: return null
            return try {
                gson.fromJson(json, UserDto::class.java)
            } catch (e: Exception) {
                null
            }
        }
        set(value) {
            if (value != null) {
                prefs.edit().putString(KEY_USER_JSON, gson.toJson(value)).apply()
            } else {
                prefs.edit().remove(KEY_USER_JSON).apply()
            }
        }

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_ENABLED, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var isVibrateEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE_ENABLED, value).apply()

    var isSimulationMode: Boolean
        get() = prefs.getBoolean(KEY_SIMULATION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SIMULATION_ENABLED, value).apply()

    fun logout() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_JSON)
            .apply()
    }
}
