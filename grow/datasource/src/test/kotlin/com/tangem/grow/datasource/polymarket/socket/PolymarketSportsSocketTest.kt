package com.tangem.grow.datasource.polymarket.socket

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.core.socket.DefaultSocketChannelFactory
import com.tangem.core.socket.SocketEnvironment
import com.tangem.core.socket.SocketState
import com.tangem.core.socket.transport.SocketConnection
import com.tangem.core.socket.transport.SocketTransport
import com.tangem.core.socket.transport.TransportEvent
import com.tangem.grow.datasource.BuildConfig
import com.tangem.grow.datasource.polymarket.socket.models.SportsResultDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketSportsSocketTest {

    @Test
    fun `GIVEN sports broadcasts WHEN reconnecting THEN optional metadata decodes and connection state recovers`() =
        runTest {
            // Arrange
            assumeFalse(BuildConfig.MOCK_DATA_SOURCE)
            val incoming = Channel<TransportEvent>(Channel.UNLIMITED)
            val connection: SocketConnection = mockk(relaxUnitFun = true) {
                every { events } returns incoming
                every { send(any()) } returns true
            }
            val foreground = MutableStateFlow(true)
            val socket = PolymarketSportsSocketModule.providePolymarketSportsSocket(
                DefaultSocketChannelFactory(
                    SocketTransport { connection },
                    object : SocketEnvironment {
                        override fun isForeground() = foreground
                        override fun defaultNetwork() = flowOf("network")
                    },
                    NetworkJsonFactory.create(), backgroundScope,
                ),
            )
            val states = mutableListOf<SocketState>()
            backgroundScope.launch { socket.state().toList(states) }
            val results = mutableListOf<SportsResultDto>()
            val collector = backgroundScope.launch { socket.results().toList(results) }
            runCurrent()
            incoming.send(TransportEvent.Opened)
            runCurrent()

            // Act
            incoming.send(TransportEvent.Message("ping"))
            incoming.send(TransportEvent.Message("""{"metadataGameId":"missing-numeric-id","score":"0-0"}"""))
            incoming.send(TransportEvent.Message("""{"gameId":1,"score":"2-1","homeTeam":"Alpha","awayTeam":"Beta","last_update":1792407600}"""))
            runCurrent()
            foreground.value = false
            runCurrent()
            foreground.value = true
            runCurrent()
            incoming.send(TransportEvent.Opened)
            runCurrent()
            val connected = socket.state().value
            collector.cancel()
            runCurrent()
            advanceTimeBy(5001)

            // Assert
            assertThat(states.count { it == SocketState.Connected }).isEqualTo(2)
            assertThat(states).contains(SocketState.Paused(SocketState.PauseReason.Background))
            assertThat(connected).isEqualTo(SocketState.Connected)
            assertThat(socket.state().value).isEqualTo(SocketState.Idle)
            assertThat(results).containsExactly(
                SportsResultDto(1, score = "2-1", homeTeam = "Alpha", awayTeam = "Beta", lastUpdate = JsonPrimitive(1792407600)),
            )
            verify(exactly = 1) { connection.send("pong") }
        }
}