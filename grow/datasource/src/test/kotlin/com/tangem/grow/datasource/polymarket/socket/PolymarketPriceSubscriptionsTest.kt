package com.tangem.grow.datasource.polymarket.socket

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.core.socket.DefaultSocketChannelFactory
import com.tangem.core.socket.SocketEnvironment
import com.tangem.core.socket.SocketState
import com.tangem.core.socket.transport.SocketConnection
import com.tangem.core.socket.transport.SocketTransport
import com.tangem.core.socket.transport.TransportEvent
import com.tangem.grow.datasource.BuildConfig
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketPriceSubscriptionsTest {

    private val transport: SocketTransport = mockk()
    private val connection: SocketConnection = mockk(relaxUnitFun = true)
    private val incoming = Channel<TransportEvent>(Channel.UNLIMITED)
    private val sent = mutableListOf<String>()
    private var opens = 0
    private val json = NetworkJsonFactory.create()

    @BeforeEach
    fun setup() {
        every { transport.open(any()) } answers {
            opens += 1
            connection
        }
        every { connection.events } returns incoming
        every { connection.send(any()) } answers { sent.add(firstArg()) }
    }

    @Test
    fun `GIVEN observed feed WHEN the connection opens THEN it authorizes and subscribes in that order`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()
        backgroundScope.launch { subscriptions.observe(TWAP_KEY).collect {} }

        // Act
        openConnection()

        // Assert
        assertFrames(AUTH_FRAME, TWAP_SUBSCRIBE_FRAME)
    }

    @Test
    fun `GIVEN feed pinned to a provider WHEN the connection opens THEN the filter names the provider`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()
        val key = PolymarketPriceFeedKey(channel = "price.crypto", symbol = "coinusd", provider = "binance")
        backgroundScope.launch { subscriptions.observe(key).collect {} }

        // Act
        openConnection()

        // Assert
        assertFrames(
            AUTH_FRAME,
            """
                {"op":"subscribe","subscriptions":[
                  {"channel":"price.crypto","filter":{"symbol":"coinusd","provider":"binance"}}]}
            """.trimIndent(),
        )
    }

    @Test
    fun `GIVEN no credentials WHEN observed THEN the socket stays closed until they appear`() = runTest {
        // Arrange
        val credentials = MutableStateFlow<PolymarketPriceAuthDto.Credentials?>(null)
        val subscriptions = createSubscriptions(credentials)
        backgroundScope.launch { subscriptions.observe(TWAP_KEY).collect {} }
        runCurrent()
        val opensWithoutCredentials = opens

        // Act
        credentials.value = CREDENTIALS
        openConnection()

        // Assert
        assertThat(opensWithoutCredentials).isEqualTo(0)
        assertThat(opens).isEqualTo(1)
        assertFrames(AUTH_FRAME, TWAP_SUBSCRIBE_FRAME)
    }

    @Test
    fun `GIVEN open socket WHEN the credentials change THEN it reconnects and authorizes with the new ones`() =
        runTest {
            // Arrange
            val credentials = MutableStateFlow<PolymarketPriceAuthDto.Credentials?>(CREDENTIALS)
            val subscriptions = createSubscriptions(credentials)
            backgroundScope.launch { subscriptions.observe(TWAP_KEY).collect {} }
            openConnection()

            // Act
            credentials.value = CREDENTIALS.copy(apiKey = "other-key")
            openConnection()

            // Assert
            assertThat(opens).isEqualTo(2)
            assertFrames(
                AUTH_FRAME,
                TWAP_SUBSCRIBE_FRAME,
                AUTH_FRAME.replace("\"key\"", "\"other-key\""),
                TWAP_SUBSCRIBE_FRAME,
            )
        }

    @Test
    fun `GIVEN feed with a price consumer WHEN the connection opens THEN its state is delivered`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()
        val states = mutableListOf<SocketState>()
        backgroundScope.launch { subscriptions.observe(TWAP_KEY).collect {} }
        backgroundScope.launch { subscriptions.observeState(TWAP_KEY).toList(states) }

        // Act
        openConnection()

        // Assert
        assertThat(states.last()).isEqualTo(SocketState.Connected)
    }

    @Test
    fun `GIVEN price frames WHEN collected THEN only the frames of the observed feed reach the consumer`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()

        // Act
        subscriptions.observe(TWAP_KEY).test {
            openConnection()
            receive("""{"op":"authed"}""")
            receive("""{"op":"subscribed","channel":"price.crypto.twap"}""")
            receive(priceFrame(symbol = "othusd", timestamp = 1))
            receive(priceFrame(channel = "price.crypto", timestamp = 2))
            receive(priceFrame(windowSeconds = 30, timestamp = 3))
            receive(priceFrame(symbol = "COIN/USD", timestamp = 4))
            receive(
                """
                    {"v":1,"channel":"price.crypto.twap","seq":1,"ts":5,"snapshot":true,
                     "payload":{"symbol":"coinusd","window_seconds":60,"source":"chainlink",
                       "data":[{"timestamp":1000,"value":1.5,"full_accuracy_value":"1.50"},{"timestamp":2000}]}}
                """.trimIndent(),
            )

            // Assert
            assertThat(awaitItem().payload?.timestamp).isEqualTo(4.0)
            val snapshot = awaitItem().payload
            assertThat(snapshot?.points).containsExactly(
                PolymarketPriceMessageDto.Point(timestamp = 1000.0, value = 1.5, fullAccuracyValue = "1.50"),
                PolymarketPriceMessageDto.Point(timestamp = 2000.0),
            ).inOrder()
            expectNoEvents()
        }
    }

    @Test
    fun `GIVEN feed pinned to a provider WHEN another provider answers THEN its frames are dropped`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()
        val key = PolymarketPriceFeedKey(channel = "price.crypto", symbol = "coinusd", provider = "binance")

        // Act
        subscriptions.observe(key).test {
            openConnection()
            receive(priceFrame(channel = "price.crypto", windowSeconds = null, source = "chainlink", timestamp = 1))
            receive(priceFrame(channel = "price.crypto", windowSeconds = null, source = "Binance", timestamp = 2))
            receive(priceFrame(channel = "price.crypto", windowSeconds = null, source = null, timestamp = 3))

            // Assert
            assertThat(awaitItem().payload?.timestamp).isEqualTo(2.0)
            assertThat(awaitItem().payload?.timestamp).isEqualTo(3.0)
            expectNoEvents()
        }
    }

    @Test
    fun `GIVEN server errors WHEN collected THEN those of the feed's channel and of the connection are delivered`() =
        runTest {
            // Arrange
            val subscriptions = createSubscriptions()

            // Act
            subscriptions.observe(TWAP_KEY).test {
                openConnection()
                receive("""{"op":"error","channel":"price.crypto","code":"bad_filter"}""")
                receive("""{"op":"error","channel":"price.crypto.twap","code":"provider_required","status":4011}""")
                receive("""{"op":"error","code":"auth_invalid"}""")

                // Assert
                assertThat(awaitItem().code).isEqualTo("provider_required")
                assertThat(awaitItem().code).isEqualTo("auth_invalid")
                expectNoEvents()
            }
        }

    private fun TestScope.createSubscriptions(
        credentials: StateFlow<PolymarketPriceAuthDto.Credentials?> = MutableStateFlow(CREDENTIALS),
    ): PolymarketPriceSubscriptions {
        assumeFalse(BuildConfig.MOCK_DATA_SOURCE, "Real transport wiring is tested in the debug variant")
        val factory = DefaultSocketChannelFactory(
            transport = transport,
            environment = object : SocketEnvironment {
                override fun isForeground(): Flow<Boolean> = flowOf(true)
                override fun defaultNetwork(): Flow<String?> = flowOf("network")
            },
            json = json,
            scope = backgroundScope,
        )
        return PolymarketPriceSubscriptions.create(
            factory = factory,
            scope = TestAppCoroutineScope(backgroundScope.coroutineContext),
            credentials = credentials,
        )
    }

    private suspend fun TestScope.openConnection() {
        runCurrent()
        incoming.send(TransportEvent.Opened)
        runCurrent()
    }

    private suspend fun TestScope.receive(frame: String) {
        incoming.send(TransportEvent.Message(frame))
        runCurrent()
    }

    private fun priceFrame(
        channel: String = "price.crypto.twap",
        symbol: String = "coinusd",
        windowSeconds: Int? = 60,
        source: String? = "chainlink",
        timestamp: Int,
    ): String {
        val window = windowSeconds?.let { ""","window_seconds":$it""" }.orEmpty()
        val provider = source?.let { ""","source":"$it"""" }.orEmpty()
        return """
            {"v":1,"channel":"$channel","seq":1,"ts":$timestamp,
             "payload":{"symbol":"$symbol","value":1.5,"timestamp":$timestamp$window$provider}}
        """.trimIndent()
    }

    private fun assertFrames(vararg expected: String) {
        assertThat(sent.map(json::parseToJsonElement))
            .containsExactlyElementsIn(expected.map(json::parseToJsonElement))
            .inOrder()
    }

    private companion object {
        val TWAP_KEY = PolymarketPriceFeedKey(channel = "price.crypto.twap", symbol = "coinusd", windowSeconds = 60)
        val CREDENTIALS = PolymarketPriceAuthDto.Credentials(apiKey = "key", secret = "secret", passphrase = "pass")

        const val AUTH_FRAME = """{"auth":{"apiKey":"key","secret":"secret","passphrase":"pass"},"op":"auth"}"""
        const val TWAP_SUBSCRIPTIONS =
            """[{"channel":"price.crypto.twap","filter":{"symbol":"coinusd","window_seconds":60}}]"""
        const val TWAP_SUBSCRIBE_FRAME = """{"op":"subscribe","subscriptions":$TWAP_SUBSCRIPTIONS}"""
        const val TWAP_UNSUBSCRIBE_FRAME = """{"op":"unsubscribe","subscriptions":$TWAP_SUBSCRIPTIONS}"""
    }
}