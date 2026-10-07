package com.tangem.core.socket.transport

import com.tangem.core.socket.SocketClose
import kotlinx.coroutines.channels.ReceiveChannel

fun interface SocketTransport {

    fun open(request: SocketRequest): SocketConnection
}

data class SocketRequest(
    val url: String,
    val headers: Map<String, String>,
)

interface SocketConnection {

    /** Ends with exactly one [TransportEvent.Closed]. */
    val events: ReceiveChannel<TransportEvent>

    fun send(text: String): Boolean

    fun close(code: Int, reason: String)
}

sealed interface TransportEvent {

    data object Opened : TransportEvent

    data class Message(val text: String) : TransportEvent

    data class Closed(val close: SocketClose) : TransportEvent
}