package com.tangem.grow.datasource.polymarket.socket

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.core.socket.DefaultSocketChannelFactory
import com.tangem.core.socket.SocketChannelFactory
import com.tangem.core.socket.SocketEnvironment
import com.tangem.core.socket.SocketState
import com.tangem.core.socket.transport.SocketConnection
import com.tangem.core.socket.transport.SocketTransport
import com.tangem.core.socket.transport.TransportEvent
import com.tangem.grow.datasource.BuildConfig
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketMarketSubscriptionsTest {

    private val transport: SocketTransport = mockk()
    private val connection: SocketConnection = mockk(relaxUnitFun = true)
    private val incoming = Channel<TransportEvent>(Channel.UNLIMITED)
    private val sent = mutableListOf<String>()
    private val foreground = MutableStateFlow(true)
    private val json = NetworkJsonFactory.create(
        serializersModule = PolymarketMarketSocketModule.providePolymarketMarketSerializersModule().provide(),
    )

    @BeforeEach
    fun setup() {
        every { transport.open(any()) } returns connection
        every { connection.events } returns incoming
        every { connection.send(any()) } answers { sent.add(firstArg()) }
    }

    @Test
    fun `GIVEN no consumers or valid assets WHEN observing THEN no transport is created`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()
        subscriptions.observe(setOf("a"))

        // Act
        val empty = subscriptions.observe(emptySet()).toList()
        val blank = subscriptions.observe(setOf("", " ")).toList()
        runCurrent()

        // Assert
        assertThat(empty).isEmpty()
        assertThat(blank).isEmpty()
        verify(exactly = 0) { transport.open(any()) }
    }

    @Test
    fun `GIVEN market frames WHEN collected THEN arrays decode and only requested assets reach the consumer`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()

        // Act
        subscriptions.observe(setOf("a")).test {
            openConnection()
            incoming.send(TransportEvent.Message("PONG"))
            incoming.send(
                TransportEvent.Message(
                    """
                        [{"event_type":"future_event","asset_id":"a"},
                         {"event_type":"book","asset_id":"a","bids":[],"asks":[],"timestamp":"1"},
                         {"event_type":"book","asset_id":"b","bids":[],"asks":[],"timestamp":"1"},
                         {"event_type":"book","asset_id":"a"},
                         {"event_type":"price_change","timestamp":"2","price_changes":[
                           {"asset_id":"a","best_ask":"0.62"},{"asset_id":"b","best_ask":"0.38"}]}]
                    """.trimIndent(),
                ),
            )

            // Assert
            assertThat(awaitItem()).isEqualTo(PolymarketMarketEventDto.Book("a", emptyList(), emptyList(), "1"))
            assertThat(awaitItem()).isEqualTo(
                PolymarketMarketEventDto.PriceChange(
                    listOf(PolymarketMarketEventDto.Change("a", bestAsk = "0.62")), "2",
                ),
            )
            expectNoEvents()
            assertFrames("""{"assets_ids":["a"],"type":"market","custom_feature_enabled":true}""")
            verify(exactly = 1) {
                transport.open(match { it.url == "wss://ws-subscriptions-clob.polymarket.com/ws/market" })
            }
        }
    }

    @Test
    fun `GIVEN an open market channel WHEN ten seconds pass THEN text heartbeat follows the initial subscription`() =
        runTest {
            // Arrange
            val subscriptions = createSubscriptions()
            backgroundScope.launch { subscriptions.observe(setOf("a")).collect {} }
            openConnection()

            // Act
            advanceTimeBy(10.seconds)
            val beforeHeartbeat = sent.toList()
            runCurrent()

            // Assert
            assertThat(beforeHeartbeat).hasSize(1)
            assertThat(sent).containsExactly(beforeHeartbeat.single(), "PING").inOrder()
        }

    @Test
    fun `GIVEN overlapping consumers WHEN one leaves THEN only its unshared assets are unsubscribed`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()
        val first = backgroundScope.launch { subscriptions.observe(setOf("b", "a")).collect {} }
        openConnection()
        val second = backgroundScope.launch { subscriptions.observe(setOf("b", "c")).collect {} }
        runCurrent()
        advanceTimeBy(101.milliseconds)

        // Act
        first.cancel()
        runCurrent()
        advanceTimeBy(2101.milliseconds)

        // Assert
        assertFrames(
            """{"assets_ids":["a","b"],"type":"market","custom_feature_enabled":true}""",
            """{"assets_ids":["c"],"operation":"subscribe","custom_feature_enabled":true}""",
            """{"assets_ids":["a"],"operation":"unsubscribe"}""",
        )
        verify(exactly = 1) { transport.open(any()) }
        verify(exactly = 0) { connection.close(any(), any()) }
        second.cancel()
    }

    @Test
    fun `GIVEN quote and state consumers WHEN refreshing THEN the shared channel resubscribes and still closes on idle`() =
        runTest {
            // Arrange
            val subscriptions = createSubscriptions()
            val states = mutableListOf<SocketState>()
            val quotes = backgroundScope.launch { subscriptions.observe(setOf("a")).collect {} }
            val status = backgroundScope.launch { subscriptions.observeState(setOf("a")).toList(states) }
            openConnection()

            // Act
            subscriptions.resubscribe(setOf("a", "unknown"))
            runCurrent()
            quotes.cancel()
            status.cancel()
            runCurrent()
            advanceTimeBy(5001.milliseconds)

            // Assert
            assertFrames(
                """{"assets_ids":["a"],"type":"market","custom_feature_enabled":true}""",
                """{"assets_ids":["a"],"operation":"unsubscribe"}""",
                """{"assets_ids":["a"],"operation":"subscribe","custom_feature_enabled":true}""",
                """{"assets_ids":["a"],"operation":"unsubscribe"}""",
            )
            assertThat(states.last()).isEqualTo(SocketState.Connected)
            verify(exactly = 1) { transport.open(any()) }
            verify(exactly = 1) { connection.close(any(), any()) }
        }

    @Test
    fun `GIVEN a released consumer WHEN the idle timeout expires THEN the socket closes`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()
        val collector = backgroundScope.launch { subscriptions.observe(setOf("a")).collect {} }
        openConnection()

        // Act
        collector.cancel()
        runCurrent()
        advanceTimeBy(5001.milliseconds)

        // Assert
        assertFrames(
            """{"assets_ids":["a"],"type":"market","custom_feature_enabled":true}""",
            """{"assets_ids":["a"],"operation":"unsubscribe"}""",
        )
        verify(exactly = 1) { connection.close(any(), any()) }
    }

    @Test
    fun `GIVEN changed subscriptions WHEN foreground reconnects THEN the first frame restores the current set`() =
        runTest {
            // Arrange
            val subscriptions = createSubscriptions()
            val first = backgroundScope.launch { subscriptions.observe(setOf("a", "b")).collect {} }
            openConnection()
            backgroundScope.launch { subscriptions.observe(setOf("b", "c")).collect {} }
            runCurrent()
            first.cancel()
            runCurrent()
            advanceTimeBy(2101.milliseconds)

            // Act
            foreground.value = false
            runCurrent()
            sent.clear()
            foreground.value = true
            openConnection()
            advanceTimeBy(101.milliseconds)

            // Assert
            assertFrames("""{"assets_ids":["b","c"],"type":"market","custom_feature_enabled":true}""")
            verify(exactly = 2) { transport.open(any()) }
        }

    @Test
    fun `GIVEN a mocked build WHEN subscribing THEN the production channel factory is never called`() = runTest {
        // Arrange
        assumeTrue(BuildConfig.MOCK_DATA_SOURCE, "Only applicable to the mocked variant")
        val factory: SocketChannelFactory = mockk()
        val subscriptions = PolymarketMarketSocketModule.providePolymarketMarketSubscriptions(
            factory, TestAppCoroutineScope(backgroundScope.coroutineContext),
        )

        // Act
        subscriptions.observe(setOf("a")).test {
            advanceTimeBy(101.milliseconds)

            // Assert
            expectNoEvents()
            verify(exactly = 0) { factory.create(any()) }
        }
    }

    private fun TestScope.createSubscriptions(): PolymarketMarketSubscriptions {
        assumeFalse(BuildConfig.MOCK_DATA_SOURCE, "Real transport wiring is tested in the debug variant")
        val factory = DefaultSocketChannelFactory(
            transport = transport,
            environment = object : SocketEnvironment {
                override fun isForeground(): Flow<Boolean> = foreground
                override fun defaultNetwork(): Flow<String?> = flowOf("network")
            },
            json = json,
            scope = backgroundScope,
        )
        return PolymarketMarketSocketModule.providePolymarketMarketSubscriptions(
            factory, TestAppCoroutineScope(backgroundScope.coroutineContext),
        )
    }

    private suspend fun TestScope.openConnection() {
        runCurrent()
        incoming.send(TransportEvent.Opened)
        runCurrent()
    }

    private fun assertFrames(vararg expected: String) {
        assertThat(sent.map(json::parseToJsonElement))
            .containsExactlyElementsIn(expected.map(json::parseToJsonElement))
            .inOrder()
    }
}