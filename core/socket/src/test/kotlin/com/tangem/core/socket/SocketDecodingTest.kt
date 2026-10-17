package com.tangem.core.socket

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonClassDiscriminator
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SocketDecodingTest {

    private val transport = FakeSocketTransport()

    @Test
    fun `GIVEN two collectors of one type WHEN a frame arrives THEN it is decoded once`() = runTest {
        // Arrange
        val channel = createChannel()
        val deserializer = CountingDeserializer(TradeDto.serializer())
        val first = collect(channel.receive(deserializer))
        val second = collect(channel.receive(deserializer))
        transport.connections.single().serverOpens()

        // Act
        transport.connections.single().serverSends(TRADE_FRAME)
        runCurrent()

        // Assert
        assertThat(deserializer.count).isEqualTo(1)
        assertThat(first).containsExactly(TradeDto(price = "2"))
        assertThat(second).containsExactly(TradeDto(price = "2"))
    }

    @Test
    fun `GIVEN two deserializers sharing a descriptor WHEN a frame arrives THEN each decodes its own type`() =
        runTest {
            // Arrange
            val channel = createChannel()
            val trades = collect(channel.receive(TradeDto.serializer()))
            val prices = collect(channel.receive(PriceOnlyDeserializer))
            transport.connections.single().serverOpens()

            // Act
            transport.connections.single().serverSends(TRADE_FRAME)
            runCurrent()

            // Assert
            assertThat(trades).containsExactly(TradeDto(price = "2"))
            assertThat(prices).containsExactly("2")
        }

    @Test
    fun `GIVEN two configured instances of one deserializer class WHEN a frame arrives THEN each keeps its own`() =
        runTest {
            // Arrange
            val channel = createChannel()
            val plain = collect(channel.receive(PrefixedPriceDeserializer(prefix = "")))
            val prefixed = collect(channel.receive(PrefixedPriceDeserializer(prefix = "$")))
            transport.connections.single().serverOpens()

            // Act
            transport.connections.single().serverSends(TRADE_FRAME)
            runCurrent()

            // Assert
            assertThat(plain).containsExactly("2")
            assertThat(prefixed).containsExactly("$2")
        }

    @Test
    fun `GIVEN a deserializer throws an unexpected error WHEN frames arrive THEN later frames still decode`() = runTest {
        // Arrange
        val channel = createChannel()
        val prices = collect(channel.receive(FailingOnceDeserializer()))
        transport.connections.single().serverOpens()

        // Act
        transport.connections.single().serverSends(TRADE_FRAME)
        transport.connections.single().serverSends(TRADE_FRAME)
        runCurrent()

        // Assert
        assertThat(prices).containsExactly("2")
    }

    @Test
    fun `GIVEN loose DTO WHEN a frame of another type arrives THEN it is decoded as that DTO`() = runTest {
        // Arrange
        val channel = createChannel()
        val trades = collect(channel.receive(TradeDto.serializer()))
        transport.connections.single().serverOpens()

        // Act
        transport.connections.single().serverSends(TICKER_FRAME)
        runCurrent()

        // Assert
        assertThat(trades).containsExactly(TradeDto(price = "1"))
    }

    @Test
    fun `GIVEN sealed base WHEN filtered by subtype THEN frames of another type are not delivered`() = runTest {
        // Arrange
        val channel = createChannel()
        val trades = collect(channel.receive(EventDto.serializer()).filterIsInstance<EventDto.Trade>())
        transport.connections.single().serverOpens()

        // Act
        transport.connections.single().serverSends(TICKER_FRAME)
        transport.connections.single().serverSends(TRADE_FRAME)
        runCurrent()

        // Assert
        assertThat(trades).containsExactly(EventDto.Trade(price = "2"))
    }

    private fun TestScope.createChannel(): SocketChannel = DefaultSocketChannelFactory(
        transport = transport,
        environment = SocketEnvironment.AlwaysAvailable,
        json = NetworkJsonFactory.create(),
        scope = backgroundScope,
    ).create(SocketSpec(url = "wss://example.com/ws"))

    private fun <T> TestScope.collect(flow: Flow<T>): List<T> {
        val received = mutableListOf<T>()
        backgroundScope.launch { flow.toList(received) }
        runCurrent()
        return received
    }

    @Serializable
    private data class TradeDto(@SerialName("price") val price: String)

    @Serializable
    @OptIn(ExperimentalSerializationApi::class)
    @JsonClassDiscriminator("type")
    private sealed interface EventDto {

        @Serializable
        @SerialName("ticker")
        data class Ticker(@SerialName("symbol") val symbol: String, @SerialName("price") val price: String) : EventDto

        @Serializable
        @SerialName("trade")
        data class Trade(@SerialName("price") val price: String) : EventDto
    }

    private object PriceOnlyDeserializer : DeserializationStrategy<String> {

        override val descriptor = TradeDto.serializer().descriptor

        override fun deserialize(decoder: Decoder): String = TradeDto.serializer().deserialize(decoder).price
    }

    private class PrefixedPriceDeserializer(private val prefix: String) : DeserializationStrategy<String> {

        override val descriptor = TradeDto.serializer().descriptor

        override fun deserialize(decoder: Decoder): String = prefix + TradeDto.serializer().deserialize(decoder).price
    }

    private class FailingOnceDeserializer : DeserializationStrategy<String> {

        private var hasFailed = false

        override val descriptor = TradeDto.serializer().descriptor

        override fun deserialize(decoder: Decoder): String {
            val price = TradeDto.serializer().deserialize(decoder).price
            if (!hasFailed) {
                hasFailed = true
                error("Unexpected state")
            }
            return price
        }
    }

    private class CountingDeserializer<T>(private val delegate: KSerializer<T>) : DeserializationStrategy<T> by delegate {

        var count = 0
            private set

        override fun deserialize(decoder: Decoder): T = delegate.deserialize(decoder).also { count++ }
    }

    private companion object {
        const val TICKER_FRAME = """{"type":"ticker","symbol":"BTC","price":"1"}"""
        const val TRADE_FRAME = """{"type":"trade","price":"2"}"""
    }
}