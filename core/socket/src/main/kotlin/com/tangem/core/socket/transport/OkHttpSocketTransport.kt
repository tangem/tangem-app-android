package com.tangem.core.socket.transport

import com.tangem.core.socket.SocketClose
import com.tangem.core.socket.SocketCloseCode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * @param client set `pingInterval` on it: OkHttp drops the read timeout once the socket is upgraded, so pings are
 * what detects a silently dead connection
 */
class OkHttpSocketTransport(private val client: OkHttpClient) : SocketTransport {

    init {
        require(client.pingIntervalMillis > 0) { "Set pingInterval on the socket client" }
    }

    override fun open(request: SocketRequest): SocketConnection {
        val events = Channel<TransportEvent>(Channel.UNLIMITED)
        val httpRequest = Request.Builder()
            .url(request.url)
            .apply { request.headers.forEach { (name, value) -> header(name, value) } }
            .build()
        val listener = EventsListener(events)
        val webSocket = client.newWebSocket(httpRequest, listener)
        return OkHttpSocketConnection(webSocket, events, listener)
    }
}

private class OkHttpSocketConnection(
    private val webSocket: WebSocket,
    override val events: ReceiveChannel<TransportEvent>,
    private val listener: EventsListener,
) : SocketConnection {

    override fun send(text: String): Boolean = webSocket.send(text)

    override fun close(code: Int, reason: String) {
        if (listener.isOpened) webSocket.close(code, reason) else webSocket.cancel()
        events.cancel()
    }
}

private class EventsListener(private val events: SendChannel<TransportEvent>) : WebSocketListener() {

    @Volatile
    var isOpened = false
        private set

    override fun onOpen(webSocket: WebSocket, response: Response) {
        isOpened = true
        events.trySend(TransportEvent.Opened)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        events.trySend(TransportEvent.Message(text))
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        events.trySend(TransportEvent.Message(bytes.utf8()))
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(SocketCloseCode.NORMAL, null)
        finish(SocketClose(code, reason))
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        finish(SocketClose(code, reason))
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        finish(
            SocketClose(
                code = SocketCloseCode.ABNORMAL,
                reason = t.message ?: t.javaClass.simpleName,
                handshakeStatus = response?.code,
            ),
        )
    }

    private fun finish(close: SocketClose) {
        events.trySend(TransportEvent.Closed(close))
        events.close()
    }
}