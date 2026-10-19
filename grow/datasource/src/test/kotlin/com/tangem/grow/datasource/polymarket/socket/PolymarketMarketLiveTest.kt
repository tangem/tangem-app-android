package com.tangem.grow.datasource.polymarket.socket

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.core.socket.DefaultSocketChannelFactory
import com.tangem.core.socket.SocketChannel
import com.tangem.core.socket.SocketChannelFactory
import com.tangem.core.socket.SocketEnvironment
import com.tangem.core.socket.SocketState
import com.tangem.core.socket.transport.OkHttpSocketTransport
import com.tangem.core.socket.transport.SocketTransport
import com.tangem.grow.datasource.BuildConfig
import com.tangem.test.core.TestAppCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Opt-in check against the public market server. Set POLYMARKET_SMOKE_ASSET_IDS to two comma-separated,
 * currently tradable token IDs and run this class with testDebugUnitTest --rerun-tasks.
 * No credentials or wallet are needed. Ordinary unit-test runs skip this check.
 * For actively updating markets, POLYMARKET_SMOKE_REQUIRE_BEST_QUOTES=true also requires best_bid_ask
 * after the initial subscription, adding an asset, resubscribing and reconnecting.
 */
@Tag("live")
internal class PolymarketMarketLiveTest {

    @Test
    @Timeout(90)
    fun `GIVEN live markets WHEN subscriptions change and reconnect THEN real snapshots resume`() = runBlocking {
        // Arrange
        val input = System.getenv("POLYMARKET_SMOKE_ASSET_IDS")
        assumeTrue(!input.isNullOrBlank(), "Set POLYMARKET_SMOKE_ASSET_IDS to opt into the live server check")
        assumeFalse(BuildConfig.MOCK_DATA_SOURCE, "Run this check in the debug variant")
        val requireBestQuotes = System.getenv("POLYMARKET_SMOKE_REQUIRE_BEST_QUOTES").toBoolean()
        val assetIds = requireNotNull(input).split(',').map(String::trim)
        require(assetIds.size == 2 && assetIds.distinct().size == 2 && assetIds.all { it.matches(Regex("[0-9]+")) }) {
            "POLYMARKET_SMOKE_ASSET_IDS must contain two different numeric token IDs"
        }
        val client = OkHttpClient.Builder().pingInterval(15, TimeUnit.SECONDS).build()
        val scope = CoroutineScope(coroutineContext + Job(coroutineContext.job))
        val foreground = MutableStateFlow(true)
        val channels = mutableListOf<SocketChannel>()
        val transport = OkHttpSocketTransport(client)
        var connectionsOpened = 0
        val factory = DefaultSocketChannelFactory(
            transport = SocketTransport { request ->
                connectionsOpened++
                transport.open(request)
            },
            environment = object : SocketEnvironment {
                override fun isForeground(): Flow<Boolean> = foreground
                override fun defaultNetwork(): Flow<String?> = flowOf("live-test")
            },
            json = NetworkJsonFactory.create(
                serializersModule = PolymarketMarketSocketModule.providePolymarketMarketSerializersModule().provide(),
            ),
            scope = scope,
        )
        val subscriptions = PolymarketMarketSocketModule.providePolymarketMarketSubscriptions(
            factory = SocketChannelFactory { spec -> factory.create(spec).also(channels::add) },
            appScope = TestAppCoroutineScope(scope.coroutineContext),
        )
        try {
            withTimeout(60.seconds) {
                // Act
                val first = scope.collectAsset(subscriptions, assetIds[0])
                val initial = first.awaitBook()
                if (requireBestQuotes) first.awaitBestBidAsk()
                val second = scope.collectAsset(subscriptions, assetIds[1])
                val added = second.awaitBook()
                if (requireBestQuotes) second.awaitBestBidAsk()
                val channel = channels.single()

                // Allow the production text heartbeat to run before requesting another server response.
                delay(12.seconds)
                first.job.cancelAndJoin()
                delay(3.seconds)
                val resubscribed = scope.collectAsset(subscriptions, assetIds[0])
                val afterHeartbeat = resubscribed.awaitBook()
                if (requireBestQuotes) resubscribed.awaitBestBidAsk()
                assertThat(connectionsOpened).isEqualTo(1)
                println("Live market: initial, added and resubscribed assets received snapshots")

                foreground.value = false
                channel.state.first { it == SocketState.Paused(SocketState.PauseReason.Background) }
                second.discardBufferedEvents()
                resubscribed.discardBufferedEvents()
                foreground.value = true
                val restoredSecond = second.awaitBook()
                val restoredFirst = resubscribed.awaitBook()
                if (requireBestQuotes) {
                    second.awaitBestBidAsk()
                    resubscribed.awaitBestBidAsk()
                }
                println("Live market: both subscriptions restored after foreground reconnect")

                second.job.cancelAndJoin()
                resubscribed.job.cancelAndJoin()
                channel.state.first { it == SocketState.Idle }

                // Assert
                assertThat(channels).hasSize(1)
                assertThat(connectionsOpened).isEqualTo(2)
                assertThat(listOf(initial.assetId, afterHeartbeat.assetId, restoredFirst.assetId))
                    .containsExactly(assetIds[0], assetIds[0], assetIds[0])
                assertThat(listOf(added.assetId, restoredSecond.assetId))
                    .containsExactly(assetIds[1], assetIds[1])
                assertThat(channel.state.value).isEqualTo(SocketState.Idle)
                println("Live events: first=${first.counts}, second=${second.counts}, restored=${resubscribed.counts}")
                println("Live market: collectors cancelled, channel returned to Idle")
            }
        } finally {
            withContext(NonCancellable) {
                scope.coroutineContext.job.cancelAndJoin()
                client.dispatcher.cancelAll()
                client.connectionPool.evictAll()
                client.dispatcher.executorService.shutdown()
            }
        }
    }

    private fun CoroutineScope.collectAsset(
        subscriptions: PolymarketMarketSubscriptions,
        assetId: String,
    ): AssetCollector {
        val events = Channel<PolymarketMarketEventDto>(Channel.UNLIMITED)
        val counts = mutableMapOf<String, Int>()
        val job = launch {
            subscriptions.observe(setOf(assetId)).collect { event ->
                val type = event.javaClass.simpleName
                counts[type] = counts.getOrDefault(type, 0) + 1
                when (event) {
                    is PolymarketMarketEventDto.Book -> assertThat(event.assetId).isEqualTo(assetId)
                    is PolymarketMarketEventDto.BestBidAsk -> assertThat(event.assetId).isEqualTo(assetId)
                    is PolymarketMarketEventDto.LastTradePrice -> assertThat(event.assetId).isEqualTo(assetId)
                    is PolymarketMarketEventDto.PriceChange ->
                        assertThat(event.priceChanges.map { it.assetId }.toSet()).containsExactly(assetId)
                    is PolymarketMarketEventDto.Unknown -> error("Unknown event reached a subscriber")
                }
                events.send(event)
            }
        }
        job.invokeOnCompletion { events.close(it) }
        return AssetCollector(job, events, counts)
    }

    private class AssetCollector(
        val job: Job,
        private val events: Channel<PolymarketMarketEventDto>,
        val counts: Map<String, Int>,
    ) {

        suspend fun awaitBook(): PolymarketMarketEventDto.Book {
            while (true) {
                val event = events.receive()
                if (event is PolymarketMarketEventDto.Book) return event
            }
        }

        suspend fun awaitBestBidAsk(): PolymarketMarketEventDto.BestBidAsk {
            while (true) {
                val event = events.receive()
                if (event is PolymarketMarketEventDto.BestBidAsk) return event
            }
        }

        fun discardBufferedEvents() {
            while (events.tryReceive().isSuccess) Unit
        }
    }
}