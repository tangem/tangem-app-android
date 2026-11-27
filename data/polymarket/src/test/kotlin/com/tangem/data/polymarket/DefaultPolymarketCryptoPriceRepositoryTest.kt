package com.tangem.data.polymarket

import com.google.common.truth.Truth.assertThat
import com.tangem.core.socket.SocketState
import com.tangem.data.polymarket.price.DefaultPolymarketPriceCredentialsSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceFeed
import com.tangem.domain.polymarket.model.PolymarketCryptoPricePoint
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceSeries
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceSeries.Status
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceAuthDto
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceFeedKey
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceMessageDto
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceSubscriptions
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultPolymarketCryptoPriceRepositoryTest {

    private val subscriptions: PolymarketPriceSubscriptions = mockk(relaxUnitFun = true)
    private val credentialsStore: PolymarketCredentialsStore = mockk()
    private val messages = MutableSharedFlow<PolymarketPriceMessageDto>()
    private val states = MutableStateFlow<SocketState>(SocketState.Idle)
    private val isStored = MutableStateFlow(true)

    @BeforeEach
    fun setup() {
        every { subscriptions.observe(any()) } returns messages
        every { subscriptions.observeState(any()) } returns states
        every { credentialsStore.hasCredentials(WALLET) } returns isStored
        coEvery { credentialsStore.get(WALLET) } returns PolymarketApiCredentials("key", "secret", "pass")
    }

    @Test
    fun `GIVEN stored credentials WHEN prices are collected THEN the socket is authorized and the series grows`() =
        runTest {
            // Arrange
            val credentialsSource = createCredentialsSource()
            val repository = createRepository(credentialsSource)
            val values = mutableListOf<PolymarketCryptoPriceSeries>()
            backgroundScope.launch { repository.observePrices(WALLET, FEED).toList(values) }
            runCurrent()
            val authorization = credentialsSource.credentials.value

            // Act
            messages.emit(snapshot(1_000.0 to "1.5", 2_000.0 to "2.5"))
            runCurrent()
            messages.emit(tick(timestamp = 3_000.0, value = "3.5"))
            runCurrent()
            messages.emit(tick(timestamp = 3_000.0, value = "9.9"))
            runCurrent()

            // Assert
            assertThat(authorization).isEqualTo(PolymarketPriceAuthDto.Credentials("key", "secret", "pass"))
            assertThat(values).containsExactly(
                PolymarketCryptoPriceSeries(),
                live(point(1_000, "1.5"), point(2_000, "2.5")),
                live(point(1_000, "1.5"), point(2_000, "2.5"), point(3_000, "3.5")),
            ).inOrder()
            verify(exactly = 0) { subscriptions.resubscribe(any()) }
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN feed WHEN prices are collected THEN its stream is observed`(model: FeedModel) = runTest {
        // Act
        backgroundScope.launch { createRepository().observePrices(WALLET, model.feed).collect {} }
        runCurrent()

        // Assert
        verify(exactly = 1) { subscriptions.observe(model.expected) }
        verify(exactly = 1) { subscriptions.observeState(model.expected) }
    }

    @Test
    fun `GIVEN stream that is already observed WHEN prices are collected THEN its snapshot is asked for once`() =
        runTest {
            // Arrange
            val values = mutableListOf<PolymarketCryptoPriceSeries>()
            backgroundScope.launch { createRepository().observePrices(WALLET, FEED).toList(values) }
            runCurrent()

            // Act
            messages.emit(tick(timestamp = 1_000.0, value = "unreadable"))
            runCurrent()
            messages.emit(tick(timestamp = 2_000.0, value = "2.5"))
            runCurrent()
            messages.emit(tick(timestamp = 3_000.0, value = "3.5"))
            runCurrent()
            messages.emit(snapshot(1_000.0 to "1.0", 2_000.0 to "2.0", 3_000.0 to "3.0"))
            runCurrent()

            // Assert
            verify(exactly = 1) { subscriptions.resubscribe(KEY) }
            assertThat(values.last()).isEqualTo(live(point(1_000, "1.0"), point(2_000, "2.0"), point(3_000, "3.0")))
        }

    @Test
    fun `GIVEN no credentials WHEN prices are collected THEN the series is unavailable and the socket untouched`() =
        runTest {
            // Arrange
            isStored.value = false
            val credentialsSource = createCredentialsSource()
            val repository = createRepository(credentialsSource)
            val values = mutableListOf<PolymarketCryptoPriceSeries>()

            // Act
            backgroundScope.launch { repository.observePrices(WALLET, FEED).toList(values) }
            runCurrent()

            // Assert
            assertThat(values).containsExactly(PolymarketCryptoPriceSeries(status = Status.UNAVAILABLE))
            assertThat(credentialsSource.credentials.value).isNull()
            verify { subscriptions wasNot Called }
        }

    @Test
    fun `GIVEN credentials that cannot be read WHEN prices are collected THEN the series is unavailable`() =
        runTest {
            // Arrange
            coEvery { credentialsStore.get(WALLET) } throws IllegalStateException("keystore")
            val values = mutableListOf<PolymarketCryptoPriceSeries>()

            // Act
            backgroundScope.launch { createRepository().observePrices(WALLET, FEED).toList(values) }
            runCurrent()

            // Assert
            assertThat(values).containsExactly(PolymarketCryptoPriceSeries(status = Status.UNAVAILABLE))
            verify { subscriptions wasNot Called }
        }

    @Test
    fun `GIVEN collected prices WHEN the credentials are removed and stored again THEN the series restarts`() =
        runTest {
            // Arrange
            val values = mutableListOf<PolymarketCryptoPriceSeries>()
            backgroundScope.launch { createRepository().observePrices(WALLET, FEED).toList(values) }
            runCurrent()
            messages.emit(snapshot(1_000.0 to "1.5"))
            runCurrent()

            // Act
            isStored.value = false
            runCurrent()
            isStored.value = true
            runCurrent()

            // Assert
            assertThat(values).containsExactly(
                PolymarketCryptoPriceSeries(),
                live(point(1_000, "1.5")),
                PolymarketCryptoPriceSeries(status = Status.UNAVAILABLE),
                PolymarketCryptoPriceSeries(),
            ).inOrder()
        }

    @Test
    fun `GIVEN collected prices WHEN the socket is stopped THEN the series is unavailable and keeps its points`() =
        runTest {
            // Arrange
            val values = mutableListOf<PolymarketCryptoPriceSeries>()
            backgroundScope.launch { createRepository().observePrices(WALLET, FEED).toList(values) }
            runCurrent()
            messages.emit(snapshot(1_000.0 to "1.5"))
            runCurrent()

            // Act
            states.value = SocketState.Stopped(code = 4001, reason = "")
            runCurrent()

            // Assert
            assertThat(values.last()).isEqualTo(live(point(1_000, "1.5")).copy(status = Status.UNAVAILABLE))
        }

    @Test
    fun `GIVEN collected prices WHEN the server refuses the subscription THEN the series is unavailable`() =
        runTest {
            // Arrange
            val values = mutableListOf<PolymarketCryptoPriceSeries>()
            backgroundScope.launch { createRepository().observePrices(WALLET, FEED).toList(values) }
            runCurrent()

            // Act
            messages.emit(PolymarketPriceMessageDto(op = "error", code = "provider_required", channel = FEED.channel))
            runCurrent()

            // Assert
            assertThat(values.last()).isEqualTo(PolymarketCryptoPriceSeries(status = Status.UNAVAILABLE))
        }

    private fun TestScope.createRepository(
        credentialsSource: DefaultPolymarketPriceCredentialsSource = createCredentialsSource(),
    ) = DefaultPolymarketCryptoPriceRepository(
        subscriptions = subscriptions,
        credentialsSource = credentialsSource,
        credentialsStore = credentialsStore,
        dispatchers = createTestingCoroutineDispatcherProvider(),
    )

    /** Watched the way the socket watches its credentials while it has collectors. */
    private fun TestScope.createCredentialsSource(): DefaultPolymarketPriceCredentialsSource {
        val source = DefaultPolymarketPriceCredentialsSource(TestAppCoroutineScope(backgroundScope.coroutineContext))
        backgroundScope.launch { source.credentials.collect {} }
        return source
    }

    private fun snapshot(vararg points: Pair<Double, String>) = PolymarketPriceMessageDto(
        channel = FEED.channel,
        payload = PolymarketPriceMessageDto.Payload(
            symbol = FEED.symbol,
            points = points.map { (timestamp, value) ->
                PolymarketPriceMessageDto.Point(timestamp = timestamp, fullAccuracyValue = value)
            },
        ),
    )

    private fun tick(timestamp: Double, value: String) = PolymarketPriceMessageDto(
        channel = FEED.channel,
        payload = PolymarketPriceMessageDto.Payload(
            symbol = FEED.symbol,
            timestamp = timestamp,
            fullAccuracyValue = value,
        ),
    )

    private fun point(timestampMillis: Long, price: String) =
        PolymarketCryptoPricePoint(timestampMillis, BigDecimal(price))

    private fun live(vararg points: PolymarketCryptoPricePoint) =
        PolymarketCryptoPriceSeries(points.toList(), Status.LIVE)

    data class FeedModel(val feed: PolymarketCryptoPriceFeed, val expected: PolymarketPriceFeedKey)

    private companion object {
        val WALLET = UserWalletId("7CE25DC32EF792CFC32380007A4172F5B64F67E4F91D37F14B351A76DAFA33DA")
        val FEED = PolymarketCryptoPriceFeed(
            channel = "price.crypto.twap",
            symbol = "coinusd",
            provider = null,
            windowSeconds = 60,
        )
        val KEY = PolymarketPriceFeedKey(channel = "price.crypto.twap", symbol = "coinusd", windowSeconds = 60)

        @JvmStatic
        fun provideTestModels() = listOf(
            FeedModel(feed = FEED, expected = KEY),
            FeedModel(
                feed = PolymarketCryptoPriceFeed(
                    channel = "price.crypto",
                    symbol = "coinusd",
                    provider = "exchange",
                    windowSeconds = null,
                ),
                expected = PolymarketPriceFeedKey(channel = "price.crypto", symbol = "coinusd", provider = "exchange"),
            ),
        )
    }
}