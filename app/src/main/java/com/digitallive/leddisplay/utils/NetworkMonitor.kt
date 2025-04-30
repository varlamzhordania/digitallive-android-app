package com.digitallive.leddisplay.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.util.Log
import com.digitallive.leddisplay.MainApplication.Companion.appContext

class NetworkMonitor() {

    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var onNetworkAvailable: (Network) -> Unit = {}
    private var onNetworkLost: (Network) -> Unit = {}

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d("NetworkMonitor", "Network available")
            onNetworkAvailable(network)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d("NetworkMonitor", "Network lost")
            onNetworkLost(network)
        }
    }

    fun setOnNetworkAvailable(onNetworkAvailable: (Network) -> Unit) {
        this.onNetworkAvailable = onNetworkAvailable
    }

    fun setOnNetworkLost(onNetworkLost: (Network) -> Unit) {
        this.onNetworkLost = onNetworkLost
    }

    fun register() {
        val request = NetworkRequest.Builder().build()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
    }
}