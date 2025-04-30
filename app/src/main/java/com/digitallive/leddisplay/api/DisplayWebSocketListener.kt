package com.digitallive.leddisplay.api

import com.digitallive.leddisplay.data.Constants.WEBSOCKET_MESSAGE_KEY
import com.digitallive.leddisplay.data.Constants.WEBSOCKET_MESSAGE_VALUE
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class DisplayWebSocketListener(
    private val onConnectionOpened: (WebSocket, Response) -> Unit,
    private val onMessageReceived: (WebSocket, String) -> Unit,
    private val onConnectionFailure: (WebSocket, Throwable, Response?) -> Unit
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocket.send(
            JSONObject().apply {
                put(WEBSOCKET_MESSAGE_KEY, WEBSOCKET_MESSAGE_VALUE)
            }.toString()
        )
        onConnectionOpened(webSocket, response)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        onMessageReceived(webSocket, text)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        onConnectionFailure(webSocket, t, response)
    }
}