package com.tangem.core.socket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

interface SocketSession {

    /** @return true when the frame was queued, not when it was delivered */
    fun send(text: String): Boolean

    fun <T> send(serializer: SerializationStrategy<T>, value: T): Boolean
}

interface SocketChannel : SocketSession {

    /** Observing the state does not hold the connection open. */
    val state: StateFlow<SocketState>

    /**
     * Holds the connection open while collected. A frame is decoded once per [deserializer] instance and shared by
     * every collector passing that instance; frames that do not decode as [T] are skipped.
     */
    fun <T> receive(deserializer: DeserializationStrategy<T>): Flow<T>
}

fun interface SocketChannelFactory {

    fun create(spec: SocketSpec): SocketChannel
}