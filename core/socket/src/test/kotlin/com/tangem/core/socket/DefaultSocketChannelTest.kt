package com.tangem.core.socket

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultSocketChannelTest {

    private val transport = FakeSocketTransport()
    private val environment = FakeSocketEnvironment()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GIVEN no collectors WHEN channel created THEN no connection is opened`() = runTest {
        // Arrange
        val channel = createChannel(SocketSpec(url = URL))

        // Act
        runCurrent()

        // Assert
        assertThat(transport.connections).isEmpty()
        assertThat(channel.state.value).isEqualTo(SocketState.Idle)
    }

    @Test
    fun `GIVEN collector WHEN socket opens THEN onOpen frame goes first and messages are delivered`() = runTest {
        // Arrange
        val channel = createChannel(SocketSpec(url = URL, onOpen = { send(SUBSCRIBE) }))
        val received = collect(channel)

        // Act
        val connection = transport.connections.single()
        connection.serverOpens()
        connection.serverSends("""{"price":"0.42"}""")
        runCurrent()

        // Assert
        assertThat(connection.request.url).isEqualTo(URL)
        assertThat(connection.sent).containsExactly(SUBSCRIBE)
        assertThat(received).containsExactly(PriceDto("0.42"))
        assertThat(channel.state.value).isEqualTo(SocketState.Connected)
    }

    @Test
    fun `GIVEN retry policy WHEN server closes THEN reconnects after backoff and repeats onOpen`() = runTest {
        // Arrange
        val channel = createChannel(
            SocketSpec(url = URL, backoff = Backoff { 3.seconds }, onOpen = { send(SUBSCRIBE) }),
        )
        collect(channel)
        transport.connections.single().serverOpens()
        runCurrent()

        // Act
        transport.connections.single().serverCloses(code = 1001)
        runCurrent()
        val waitingState = channel.state.value
        advanceTimeBy(3.seconds + 1.milliseconds)
        transport.connections.last().serverOpens()
        runCurrent()

        // Assert
        assertThat(waitingState).isEqualTo(SocketState.WaitingToRetry(attempt = 1, delay = 3.seconds))
        assertThat(transport.connections).hasSize(2)
        assertThat(transport.connections.last().sent).containsExactly(SUBSCRIBE)
        assertThat(channel.state.value).isEqualTo(SocketState.Connected)
    }

    @Test
    fun `GIVEN stop policy WHEN server closes THEN channel stops without reconnecting`() = runTest {
        // Arrange
        val channel = createChannel(SocketSpec(url = URL, closePolicy = { ClosePolicy.Decision.Stop }))
        collect(channel)
        transport.connections.single().serverOpens()
        runCurrent()

        // Act
        transport.connections.single().serverCloses(code = 4008, reason = "do not reconnect")
        advanceTimeBy(1.seconds * 60)

        // Assert
        assertThat(channel.state.value).isEqualTo(SocketState.Stopped(code = 4008, reason = "do not reconnect"))
        assertThat(transport.connections).hasSize(1)
    }

    @Test
    fun `GIVEN connected socket WHEN app goes to background and back THEN pauses and reconnects without backoff`() =
        runTest {
            // Arrange
            val channel = createChannel(SocketSpec(url = URL, backoff = Backoff { 30.seconds }))
            collect(channel)
            transport.connections.single().serverOpens()
            runCurrent()

            // Act
            environment.foreground.value = false
            runCurrent()
            val backgroundState = channel.state.value
            environment.foreground.value = true
            runCurrent()

            // Assert
            assertThat(backgroundState).isEqualTo(SocketState.Paused(SocketState.PauseReason.Background))
            assertThat(transport.connections.first().closedByClient).isTrue()
            assertThat(transport.connections).hasSize(2)
            assertThat(channel.state.value).isEqualTo(SocketState.Connecting(attempt = 0))
        }

    @Test
    fun `GIVEN no network WHEN collector arrives THEN pauses without opening a connection`() = runTest {
        // Arrange
        environment.network.value = null
        val channel = createChannel(SocketSpec(url = URL))

        // Act
        collect(channel)

        // Assert
        assertThat(transport.connections).isEmpty()
        assertThat(channel.state.value).isEqualTo(SocketState.Paused(SocketState.PauseReason.NoNetwork))
    }

    @Test
    fun `GIVEN last collector leaves WHEN idle timeout passes THEN connection is closed`() = runTest {
        // Arrange
        val channel = createChannel(SocketSpec(url = URL, idleTimeout = 5.seconds))
        val collector = launch { channel.receive(PriceDto.serializer()).collect {} }
        runCurrent()
        transport.connections.single().serverOpens()
        runCurrent()

        // Act
        collector.cancel()
        advanceTimeBy(4.seconds)
        val closedBeforeTimeout = transport.connections.single().closedByClient
        advanceTimeBy(1.seconds + 1.milliseconds)

        // Assert
        assertThat(closedBeforeTimeout).isFalse()
        assertThat(transport.connections.single().closedByClient).isTrue()
        assertThat(channel.state.value).isEqualTo(SocketState.Idle)
    }

    @Test
    fun `GIVEN client keep-alive WHEN connected THEN first ping goes out a full interval after onOpen`() = runTest {
        // Arrange
        val channel = createChannel(
            SocketSpec(url = URL, keepAlive = KeepAlive.ClientSends("PING", 10.seconds), onOpen = { send(SUBSCRIBE) }),
        )
        collect(channel)
        val connection = transport.connections.single()
        connection.serverOpens()
        runCurrent()

        // Act
        advanceTimeBy(9.seconds)
        val sentBeforeInterval = connection.sent.toList()
        advanceTimeBy(1.seconds + 1.milliseconds)

        // Assert
        assertThat(sentBeforeInterval).containsExactly(SUBSCRIBE)
        assertThat(connection.sent).containsExactly(SUBSCRIBE, "PING").inOrder()
    }

    @Test
    fun `GIVEN server keep-alive WHEN server pings THEN replies and does not deliver the ping`() = runTest {
        // Arrange
        val channel = createChannel(SocketSpec(url = URL, keepAlive = KeepAlive.ServerSends("ping", "pong")))
        val received = collect(channel)
        val connection = transport.connections.single()
        connection.serverOpens()

        // Act
        connection.serverSends("ping")
        runCurrent()

        // Assert
        assertThat(connection.sent).containsExactly("pong")
        assertThat(received).isEmpty()
    }

    @Test
    fun `GIVEN array frames split WHEN mixed frames arrive THEN only decodable elements are delivered`() = runTest {
        // Arrange
        val channel = createChannel(SocketSpec(url = URL, isSplitArrayFrames = true))
        val received = collect(channel)
        val connection = transport.connections.single()
        connection.serverOpens()

        // Act
        connection.serverSends("""[{"price":"0.1"},{"other":1},{"price":"0.2"}]""")
        connection.serverSends("PONG")
        connection.serverSends("{broken")
        connection.serverSends("""{"price":"0.3"}""")
        runCurrent()

        // Assert
        assertThat(received).containsExactly(PriceDto("0.1"), PriceDto("0.2"), PriceDto("0.3")).inOrder()
    }

    @Test
    fun `GIVEN channel lifecycle turns inactive WHEN connected THEN connection closes and state is idle`() = runTest {
        // Arrange
        val isActive = MutableStateFlow(true)
        val channel = createChannel(SocketSpec(url = URL, lifecycle = { isActive }))
        collect(channel)
        transport.connections.single().serverOpens()
        runCurrent()

        // Act
        isActive.value = false
        runCurrent()

        // Assert
        assertThat(transport.connections.single().closedByClient).isTrue()
        assertThat(channel.state.value).isEqualTo(SocketState.Idle)
    }

    @Test
    fun `GIVEN handshake rejected with 401 WHEN retrying THEN headers are resolved again with the rejection`() =
        runTest {
            // Arrange
            val contexts = mutableListOf<SocketConnectContext>()
            val channel = createChannel(
                SocketSpec(
                    url = URL,
                    backoff = Backoff { 1.seconds },
                    headers = { context ->
                        contexts += context
                        val token = if (context.previousClose?.handshakeStatus == 401) "fresh" else "stale"
                        mapOf("Authorization" to "Bearer $token")
                    },
                ),
            )
            collect(channel)

            // Act
            transport.connections.single().serverRejectsHandshake(status = 401)
            advanceTimeBy(1.seconds + 1.milliseconds)

            // Assert
            assertThat(transport.connections.map { it.request.headers["Authorization"] })
                .containsExactly("Bearer stale", "Bearer fresh")
                .inOrder()
            assertThat(contexts.last()).isEqualTo(
                SocketConnectContext(
                    attempt = 1,
                    previousClose = SocketClose(SocketCloseCode.ABNORMAL, "HTTP 401", handshakeStatus = 401),
                ),
            )
        }

    @Test
    fun `GIVEN headers fail WHEN connecting THEN no connection is opened and the policy retries`() = runTest {
        // Arrange
        var isTokenAvailable = false
        val channel = createChannel(
            SocketSpec(
                url = URL,
                backoff = Backoff { 1.seconds },
                headers = { mapOf("Authorization" to if (isTokenAvailable) "Bearer t" else error("no token")) },
            ),
        )

        // Act
        collect(channel)
        val stateWithoutToken = channel.state.value
        isTokenAvailable = true
        advanceTimeBy(1.seconds + 1.milliseconds)

        // Assert
        assertThat(stateWithoutToken).isEqualTo(SocketState.WaitingToRetry(attempt = 1, delay = 1.seconds))
        assertThat(transport.connections.single().request.headers).containsExactly("Authorization", "Bearer t")
    }

    @Test
    fun `GIVEN spec transport WHEN connecting THEN it replaces the factory transport`() = runTest {
        // Arrange
        val ownTransport = FakeSocketTransport()
        val channel = createChannel(SocketSpec(url = URL, transport = ownTransport))

        // Act
        collect(channel)

        // Assert
        assertThat(transport.connections).isEmpty()
        assertThat(ownTransport.connections).hasSize(1)
    }

    @Test
    fun `GIVEN event reconnect trigger without replay WHEN collector arrives THEN connects without waiting for it`() =
        runTest {
            // Arrange
            val trigger = MutableSharedFlow<Unit>()
            val channel = createChannel(SocketSpec(url = URL, reconnectOn = trigger))

            // Act
            collect(channel)
            transport.connections.single().serverOpens()
            runCurrent()
            trigger.emit(Unit)
            runCurrent()
            val connectionsAfterBaseline = transport.connections.size
            trigger.emit(Unit)
            runCurrent()

            // Assert
            assertThat(connectionsAfterBaseline).isEqualTo(1)
            assertThat(transport.connections).hasSize(2)
            assertThat(transport.connections.first().closedByClient).isTrue()
        }

    @Test
    fun `GIVEN reconnect trigger WHEN it emits THEN reconnects at once with fresh headers`() = runTest {
        // Arrange
        val account = MutableStateFlow("first")
        val channel = createChannel(
            SocketSpec(
                url = URL,
                backoff = Backoff { 30.seconds },
                reconnectOn = account,
                headers = { mapOf("X-Account" to account.value) },
            ),
        )
        collect(channel)
        transport.connections.single().serverOpens()
        runCurrent()

        // Act
        account.value = "second"
        runCurrent()

        // Assert
        assertThat(transport.connections.first().closedByClient).isTrue()
        assertThat(transport.connections.map { it.request.headers["X-Account"] })
            .containsExactly("first", "second")
            .inOrder()
    }

    @Test
    fun `GIVEN transport throws on open WHEN connecting THEN the attempt fails like a drop and is retried`() = runTest {
        // Arrange
        transport.openError = IllegalArgumentException("Unexpected char in header value")
        val channel = createChannel(SocketSpec(url = URL, backoff = Backoff { 3.seconds }))

        // Act
        collect(channel)
        val failedState = channel.state.value
        transport.openError = null
        advanceTimeBy(3.seconds + 1.milliseconds)

        // Assert
        assertThat(failedState).isEqualTo(SocketState.WaitingToRetry(attempt = 1, delay = 3.seconds))
        assertThat(transport.connections).hasSize(1)
    }

    @Test
    fun `GIVEN onOpen throws WHEN socket opens THEN connection is closed and retried`() = runTest {
        // Arrange
        var onOpenCalls = 0
        val channel = createChannel(
            SocketSpec(
                url = URL,
                backoff = Backoff { 3.seconds },
                onOpen = {
                    onOpenCalls++
                    if (onOpenCalls == 1) error("Auth frame failed")
                },
            ),
        )
        collect(channel)

        // Act
        transport.connections.single().serverOpens()
        runCurrent()
        val failedState = channel.state.value
        advanceTimeBy(3.seconds + 1.milliseconds)

        // Assert
        assertThat(failedState).isEqualTo(SocketState.WaitingToRetry(attempt = 1, delay = 3.seconds))
        assertThat(transport.connections.first().closedByClient).isTrue()
        assertThat(transport.connections).hasSize(2)
    }

    @Test
    fun `GIVEN close policy throws WHEN server closes THEN the loop restarts instead of dying`() = runTest {
        // Arrange
        var decisions = 0
        val channel = createChannel(
            SocketSpec(
                url = URL,
                closePolicy = {
                    decisions++
                    if (decisions == 1) error("Policy failed")
                    ClosePolicy.Decision.Retry
                },
            ),
        )
        collect(channel)
        transport.connections.single().serverOpens()
        runCurrent()

        // Act
        transport.connections.single().serverCloses(code = 1001)
        runCurrent()
        advanceTimeBy(5.seconds + 1.milliseconds)

        // Assert
        assertThat(transport.connections).hasSize(2)
        assertThat(channel.state.value).isEqualTo(SocketState.Connecting(attempt = 0))
    }

    @Test
    fun `GIVEN server accepts and closes at once WHEN reconnecting THEN backoff keeps growing`() = runTest {
        // Arrange
        val attempts = mutableListOf<Int>()
        val channel = createChannel(
            SocketSpec(url = URL, backoff = { attempt -> 1.seconds.also { attempts += attempt } }),
        )
        collect(channel)

        // Act
        repeat(3) {
            transport.connections.last().serverOpens()
            transport.connections.last().serverCloses(code = 1013)
            runCurrent()
            advanceTimeBy(1.seconds + 1.milliseconds)
        }

        // Assert
        assertThat(attempts).containsExactly(0, 1, 2).inOrder()
    }

    @Test
    fun `GIVEN connection delivered a message WHEN it closes THEN backoff starts over`() = runTest {
        // Arrange
        val attempts = mutableListOf<Int>()
        val channel = createChannel(
            SocketSpec(url = URL, backoff = { attempt -> 1.seconds.also { attempts += attempt } }),
        )
        collect(channel)
        repeat(2) {
            transport.connections.last().serverOpens()
            transport.connections.last().serverCloses(code = 1013)
            runCurrent()
            advanceTimeBy(1.seconds + 1.milliseconds)
        }

        // Act
        transport.connections.last().serverOpens()
        transport.connections.last().serverSends("""{"price":"0.42"}""")
        transport.connections.last().serverCloses(code = 1001)
        runCurrent()

        // Assert
        assertThat(attempts).containsExactly(0, 1, 0).inOrder()
    }

    @Test
    fun `GIVEN onOpen in progress WHEN someone sends THEN the frame goes out right after onOpen frames`() = runTest {
        // Arrange
        val onOpenDone = CompletableDeferred<Unit>()
        val channel = createChannel(
            SocketSpec(
                url = URL,
                onOpen = {
                    send(SUBSCRIBE)
                    onOpenDone.await()
                },
            ),
        )
        collect(channel)
        val connection = transport.connections.single()

        // Act
        connection.serverOpens()
        runCurrent()
        val isEarlySendQueued = channel.send("early")
        val stateDuringOnOpen = channel.state.value
        onOpenDone.complete(Unit)
        runCurrent()

        // Assert
        assertThat(isEarlySendQueued).isTrue()
        assertThat(stateDuringOnOpen).isEqualTo(SocketState.Connecting(attempt = 0))
        assertThat(connection.sent).containsExactly(SUBSCRIBE, "early").inOrder()
        assertThat(channel.state.value).isEqualTo(SocketState.Connected)
    }

    @Test
    fun `GIVEN last collector leaves WHEN a new one comes before idle timeout THEN connection is kept`() = runTest {
        // Arrange
        val channel = createChannel(SocketSpec(url = URL, idleTimeout = 5.seconds))
        val first = launch { channel.receive(PriceDto.serializer()).collect {} }
        runCurrent()
        transport.connections.single().serverOpens()
        runCurrent()

        // Act
        first.cancel()
        advanceTimeBy(3.seconds)
        collect(channel)
        advanceTimeBy(5.seconds)

        // Assert
        assertThat(transport.connections).hasSize(1)
        assertThat(transport.connections.single().closedByClient).isFalse()
        assertThat(channel.state.value).isEqualTo(SocketState.Connected)
    }

    @Test
    fun `GIVEN backoff returning a negative delay WHEN server closes THEN no tight reconnect loop starts`() = runTest {
        // Arrange
        val channel = createChannel(SocketSpec(url = URL, backoff = { (-1).seconds }))
        collect(channel)
        transport.connections.single().serverOpens()
        runCurrent()

        // Act
        transport.connections.single().serverCloses(code = 1001)
        runCurrent()
        val connectionsRightAfter = transport.connections.size
        advanceTimeBy(5.seconds + 1.milliseconds)

        // Assert
        assertThat(connectionsRightAfter).isEqualTo(1)
        assertThat(transport.connections).hasSize(2)
    }

    @Test
    fun `GIVEN negative idle timeout WHEN spec created THEN it is rejected`() {
        // Arrange
        val idleTimeout = (-1).seconds

        // Act
        val error = runCatching { SocketSpec(url = URL, idleTimeout = idleTimeout) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN retry-after policy WHEN created with a negative delay THEN it is rejected`() {
        // Arrange
        val delay = (-1).seconds

        // Act
        val error = runCatching { ClosePolicy.Decision.RetryAfter(delay) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN connected socket WHEN default network changes THEN reconnects at once without backoff`() = runTest {
        // Arrange
        val channel = createChannel(SocketSpec(url = URL, backoff = Backoff { 30.seconds }))
        collect(channel)
        transport.connections.single().serverOpens()
        runCurrent()

        // Act
        environment.network.value = CELLULAR
        runCurrent()

        // Assert
        assertThat(transport.connections.first().closedByClient).isTrue()
        assertThat(transport.connections).hasSize(2)
        assertThat(channel.state.value).isEqualTo(SocketState.Connecting(attempt = 0))
    }

    @Test
    fun `GIVEN a stuck collector WHEN frames keep coming THEN other collectors still get every frame`() = runTest {
        // Arrange
        val channel = createChannel(SocketSpec(url = URL))
        val received = collect(channel)
        backgroundScope.launch { channel.receive(PriceDto.serializer()).collect { awaitCancellation() } }
        runCurrent()
        val connection = transport.connections.single()
        connection.serverOpens()
        runCurrent()

        // Act
        repeat(FRAMES_BEYOND_BUFFERS) { index ->
            connection.serverSends("""{"price":"$index"}""")
            runCurrent()
        }

        // Assert
        assertThat(received).hasSize(FRAMES_BEYOND_BUFFERS)
    }

    @Test
    fun `GIVEN no collectors WHEN channel exists THEN it does not observe the environment`() = runTest {
        // Arrange
        val channel = createChannel(SocketSpec(url = URL, idleTimeout = 5.seconds))
        runCurrent()
        val idleSubscriptions = environment.network.subscriptionCount.value

        // Act
        val collector = launch { channel.receive(PriceDto.serializer()).collect {} }
        runCurrent()
        val activeSubscriptions = environment.network.subscriptionCount.value
        collector.cancel()
        advanceTimeBy(5.seconds + 1.milliseconds)

        // Assert
        assertThat(idleSubscriptions).isEqualTo(0)
        assertThat(activeSubscriptions).isEqualTo(1)
        assertThat(environment.network.subscriptionCount.value).isEqualTo(0)
    }

    private fun TestScope.createChannel(spec: SocketSpec): SocketChannel = DefaultSocketChannelFactory(
        transport = transport,
        environment = environment,
        json = json,
        scope = backgroundScope,
    ).create(spec)

    private fun TestScope.collect(channel: SocketChannel): List<PriceDto> {
        val received = mutableListOf<PriceDto>()
        backgroundScope.launch { channel.receive(PriceDto.serializer()).toList(received) }
        runCurrent()
        return received
    }

    @Serializable
    private data class PriceDto(val price: String)

    private class FakeSocketEnvironment : SocketEnvironment {

        val foreground = MutableStateFlow(true)
        val network = MutableStateFlow<String?>(WIFI)

        override fun isForeground() = foreground

        override fun defaultNetwork() = network
    }

    private companion object {
        const val URL = "wss://example.com/ws"
        const val WIFI = "wifi"
        const val CELLULAR = "cellular"
        const val FRAMES_BEYOND_BUFFERS = 600
        const val SUBSCRIBE = """{"type":"subscribe"}"""
    }
}