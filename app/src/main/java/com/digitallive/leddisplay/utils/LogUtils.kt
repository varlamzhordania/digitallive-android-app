package com.digitallive.leddisplay.utils

import android.util.Log
import android.widget.Toast
import com.digitallive.leddisplay.MainApplication.Companion.appContext
import com.digitallive.leddisplay.api.RetrofitClient
import com.digitallive.leddisplay.models.ErrorLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LogUtils {

    private val logChannel = Channel<ErrorLog>(Channel.UNLIMITED)

    enum class LogLevel {
        ERROR,
        WARNING,
        INFO,
        DEBUG,
        UNKNOWN
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            for (log in logChannel) {
                try {
                    val response = RetrofitClient.logApi.sendLog(ErrorLog(log.type, log.message))
                    if (response.isSuccessful) {
                        Log.d("LogUtils", "Log sent successfully")
                    } else {
                        Log.e("LogUtils", "Failed to send log: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("LogUtils", "Error sending log", e)
                    if (DataManager.getShowError()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                appContext,
                                "Error sending log: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    fun sendLog(type: LogLevel, message: String) {
        if (DataManager.getLogServerUrl().isNullOrEmpty() ||
            DataManager.getDisplayToken().isNullOrEmpty()
        ) {
            Log.w("LogUtils", "Log server URL or display token is missing")
            return
        }

        logChannel.trySend(ErrorLog(type.name, message))
    }
}