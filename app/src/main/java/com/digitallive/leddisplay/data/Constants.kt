package com.digitallive.leddisplay.data

import android.widget.Toast
import com.digitallive.leddisplay.MainApplication.Companion.appContext
import com.digitallive.leddisplay.utils.DataManager

object Constants {

    const val WEBSOCKET_MESSAGE_KEY = "action"
    const val WEBSOCKET_MESSAGE_VALUE = "get_display_data"

    fun getDisplayUrl(): String {
        val serverUrl = DataManager.getWebSocketServerUrl() ?: run {
            Toast.makeText(appContext, "Server URL not found", Toast.LENGTH_SHORT).show()
            return ""
        }
        val secretKey = DataManager.getDisplaySecretKey() ?: run {
            Toast.makeText(appContext, "Secret Key not found", Toast.LENGTH_SHORT).show()
            return ""
        }

        return "$serverUrl?code=$secretKey"
    }
}