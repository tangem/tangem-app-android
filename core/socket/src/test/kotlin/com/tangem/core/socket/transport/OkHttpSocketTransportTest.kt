package com.tangem.core.socket.transport

import com.google.common.truth.Truth.assertThat
import com.tangem.core.socket.SocketClose
import com.tangem.core.socket.SocketCloseCode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okio.ByteString
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal class OkHttpSocketTransportTest {

    @Test
    fun `GIVEN client without ping interval WHEN transport created THEN it is rejected`() {
        // Arrange
        val client = OkHttpClient()

        // Act
        val error = runCatching { OkHttpSocketTransport(client) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN client with ping interval WHEN transport created THEN it is accepted`() {
        // Arrange
        val client = OkHttpClient.Builder().pingInterval(15, TimeUnit.SECONDS).build()

        // Act
        val error = runCatching { OkHttpSocketTransport(client) }.exceptionOrNull()

        // Assert
        assertThat(error).isNull()
    }

    @Test
    fun `GIVEN full event buffer WHEN more events arrive THEN the reader waits and nothing is lost`() = runTest {
        // Arrange
        val events = Channel<TransportEvent>(capacity = 1)
        val webSocket = FakeWebSocket()
        val listener = EventsListener(events)
        val reader = thread {
            listener.onOpen(webSocket, response())
            listener.onMessage(webSocket, "1")
            listener.onMessage(webSocket, "2")
            listener.onClosed(webSocket, SocketCloseCode.NORMAL, "bye")
        }

        // Act
        val received = events.toList()
        reader.join()

        // Assert
        assertThat(received).containsExactly(
            TransportEvent.Opened,
            TransportEvent.Message("1"),
            TransportEvent.Message("2"),
            TransportEvent.Closed(SocketClose(SocketCloseCode.NORMAL, "bye")),
        ).inOrder()
    }

    @Test
    fun `GIVEN reader waiting on a full buffer WHEN consumer cancels the events THEN the reader is released`() {
        // Arrange
        val events = Channel<TransportEvent>(capacity = 1)
        val webSocket = FakeWebSocket()
        val listener = EventsListener(events)
        listener.onMessage(webSocket, "1")
        val reader = thread { listener.onMessage(webSocket, "2") }

        // Act
        events.cancel()
        reader.join(TimeUnit.SECONDS.toMillis(5))

        // Assert
        assertThat(reader.isAlive).isFalse()
    }

    private fun response(): Response = Response.Builder()
        .request(Request.Builder().url("https://example.com").build())
        .protocol(Protocol.HTTP_1_1)
        .code(101)
        .message("Switching Protocols")
        .build()

    private class FakeWebSocket : WebSocket {

        override fun cancel() = Unit

        override fun close(code: Int, reason: String?): Boolean = true

        override fun queueSize(): Long = 0

        override fun request(): Request = Request.Builder().url("https://example.com").build()

        override fun send(text: String): Boolean = true

        override fun send(bytes: ByteString): Boolean = true
    }
}