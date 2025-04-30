package com.digitallive.leddisplay.utils

import android.content.Context
import android.graphics.Color
import androidx.core.content.edit
import com.digitallive.leddisplay.MainApplication.Companion.appContext
import com.digitallive.leddisplay.models.VideoData
import com.google.gson.Gson

object DataManager {

    private const val PREF_FILE = "video_prefs"
    private const val KEY_WEBSOCKET_SERVER_URL = "websocket_server_url"
    private const val KEY_LOG_SERVER_URL = "log_server_url"
    private const val KEY_DISPLAY_SECRET_KEY = "display_secret_key"
    private const val KEY_DISPLAY_TOKEN = "display_token"
    private const val KEY_RETRY_INTERVAL = "retry_interval"
    private const val KEY_VIDEO_FILE_NAME = "video_file_name"
    private const val KEY_VIDEO_DATA = "video_data"
    private const val KEY_TICKER_BACKGROUND_COLOR = "ticker_background_color"
    private const val KEY_TICKER_TEXT_COLOR = "ticker_text_color"
    private const val KEY_SHOW_ERROR = "show_error"

    private val gson = Gson()
    private val sharedPreferences by lazy {
        appContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    fun saveWebSocketServerUrl(url: String) {
        sharedPreferences.edit {
            putString(KEY_WEBSOCKET_SERVER_URL, url)
        }
    }

    fun getWebSocketServerUrl(): String? {
        return sharedPreferences.getString(KEY_WEBSOCKET_SERVER_URL, null)
    }

    fun saveLogServerUrl(url: String) {
        sharedPreferences.edit {
            putString(KEY_LOG_SERVER_URL, url)
        }
    }

    fun getLogServerUrl(): String? {
        return sharedPreferences.getString(KEY_LOG_SERVER_URL, null)
    }

    fun saveDisplaySecretKey(key: String) {
        sharedPreferences.edit {
            putString(KEY_DISPLAY_SECRET_KEY, key)
        }
    }

    fun getDisplaySecretKey(): String? {
        return sharedPreferences.getString(KEY_DISPLAY_SECRET_KEY, null)
    }

    fun saveDisplayToken(key: String) {
        sharedPreferences.edit {
            putString(KEY_DISPLAY_TOKEN, key)
        }
    }

    fun getDisplayToken(): String? {
        return sharedPreferences.getString(KEY_DISPLAY_TOKEN, null)
    }

    fun saveRetryInterval(minute: Int) {
        sharedPreferences.edit {
            putInt(KEY_RETRY_INTERVAL, minute.coerceAtLeast(1))
        }
    }

    fun getRetryInterval(): Int {
        return sharedPreferences.getInt(KEY_RETRY_INTERVAL, 15)
    }

    fun saveVideoFileName(fileName: String) {
        sharedPreferences.edit {
            putString(KEY_VIDEO_FILE_NAME, fileName)
        }
    }

    fun getVideoFileName(): String {
        return sharedPreferences.getString(KEY_VIDEO_FILE_NAME, null) ?: "cached_video.mp4"
    }

    fun saveVideoData(data: VideoData) {
        sharedPreferences.edit {
            putString(KEY_VIDEO_DATA, gson.toJson(data))
        }
    }

    fun getVideoData(): VideoData? {
        return sharedPreferences.getString(KEY_VIDEO_DATA, null)?.let {
            try {
                gson.fromJson(it, VideoData::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun saveTickerBackgroundColor(color: Int) {
        sharedPreferences.edit {
            putInt(KEY_TICKER_BACKGROUND_COLOR, color)
        }
    }

    fun getTickerBackgroundColor(): Int {
        return sharedPreferences.getInt(KEY_TICKER_BACKGROUND_COLOR, Color.RED)
    }

    fun saveTickerTextColor(color: Int) {
        sharedPreferences.edit {
            putInt(KEY_TICKER_TEXT_COLOR, color)
        }
    }

    fun getTickerTextColor(): Int {
        return sharedPreferences.getInt(KEY_TICKER_TEXT_COLOR, Color.WHITE)
    }

    fun saveShowError(show: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_SHOW_ERROR, show)
        }
    }

    fun getShowError(): Boolean {
        return sharedPreferences.getBoolean(KEY_SHOW_ERROR, false)
    }
}