package com.tangem.core.socket

import com.tangem.core.socket.transport.SocketConnection
import com.tangem.core.socket.transport.SocketRequest
import com.tangem.core.socket.transport.SocketTransport
import com.tangem.core.socket.transport.TransportEvent
import kotlinx.coroutines.channels.Channel

internal class FakeSocketTransport : SocketTransport {

    val connections = mutableListOf<FakeSocketConnection>()

    var openError: Exception? = null

    override fun open(request: SocketRequest): SocketConnection {
        openError?.let { throw it }
        return FakeSocketConnection(request).also(connections::add)
    }
}

internal class FakeSocketConnection(val request: SocketRequest) : SocketConnection {

    override val events = Channel<TransportEvent>(Channel.UNLIMITED)

    val sent = mutableListOf<String>()

    var closedByClient = false
        private set

    fun serverOpens() {
        events.trySend(TransportEvent.Opened)
    }

    fun serverSends(text: String) {
        events.trySend(TransportEvent.Message(text))
    }

    fun serverCloses(code: Int, reason: String = "") {
        events.trySend(TransportEvent.Closed(SocketClose(code, reason)))
        events.close()
    }

    fun serverRejectsHandshake(status: Int) {
        events.trySend(TransportEvent.Closed(SocketClose(SocketCloseCode.ABNORMAL, "HTTP $status", status)))
        events.close()
    }

    override fun send(text: String): Boolean = sent.add(text)

    override fun close(code: Int, reason: String) {
        closedByClient = true
        events.close()
    }
}