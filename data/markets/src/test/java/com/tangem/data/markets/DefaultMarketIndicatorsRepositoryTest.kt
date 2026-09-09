package com.tangem.data.markets

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.store.datasource.markets.TangemTechMarketsApi
import com.tangem.store.datasource.markets.models.response.GetCoinIndicatorsResponse
import com.tangem.store.datasource.markets.models.response.GetCoinIndicatorsResponse.Asset
import com.tangem.store.datasource.markets.models.response.GetCoinIndicatorsResponse.Asset.Indicator
import com.tangem.core.local.datastore.RuntimeStateStore
import com.tangem.domain.markets.CoinIndicators
import com.tangem.domain.markets.MarketIndicators
import com.tangem.store.datasource.markets.models.response.MarketIndicatorsResponse
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMarketIndicatorsRepositoryTest {

    private val marketsApi: TangemTechMarketsApi = mockk()
    private val store: RuntimeStateStore<Map<String, CoinIndicators>> = RuntimeStateStore(defaultValue = emptyMap())

    private val repository = DefaultMarketIndicatorsRepository(
        marketsApi = marketsApi,
        store = store,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun setup() {
        clearMocks(marketsApi)
        store.clear()
    }

    @Test
    fun `GIVEN store holds readings WHEN getCoinIndicatorsUpdates THEN it reflects the store`() = runTest {
        // Arrange
        store.store(mapOf("BTC" to coinIndicators("BTC")))

        // Act
        val actual = repository.getCoinIndicatorsUpdates().first()

        // Assert
        assertThat(actual.keys).containsExactly("BTC")
    }

    @Test
    fun `GIVEN successful fetch WHEN fetchCoinIndicators THEN readings are stored keyed by uppercase symbol`() =
        runTest {
            // Arrange — backend returns a lowercase symbol; the store key must be uppercased
            coEvery { marketsApi.getCoinIndicators(any(), any()) } returns success(asset(symbol = "btc"))

            // Act
            val result = repository.fetchCoinIndicators(symbols = null, indicators = null)

            // Assert
            assertThat(result.isRight()).isTrue()
            assertThat(store.get().value.keys).containsExactly("BTC")
        }

    @Test
    fun `GIVEN existing readings WHEN fetching another symbol THEN both are kept (merge not replace)`() = runTest {
        // Arrange — a prior single-symbol refresh populated ETH; fetching BTC must not evict it
        store.store(mapOf("ETH" to coinIndicators("ETH")))
        coEvery { marketsApi.getCoinIndicators(any(), any()) } returns success(asset(symbol = "BTC"))

        // Act
        repository.fetchCoinIndicators(symbols = listOf("BTC"), indicators = null)

        // Assert
        assertThat(store.get().value.keys).containsExactly("ETH", "BTC")
    }

    @Test
    fun `GIVEN existing reading for a symbol WHEN fetching the same symbol THEN it is replaced`() = runTest {
        // Arrange
        store.store(mapOf("BTC" to coinIndicators("BTC", readingCount = 0)))
        coEvery { marketsApi.getCoinIndicators(any(), any()) } returns success(asset(symbol = "BTC"))

        // Act
        repository.fetchCoinIndicators(symbols = listOf("BTC"), indicators = null)

        // Assert — one BTC entry, now with the freshly fetched reading
        assertThat(store.get().value.keys).containsExactly("BTC")
        assertThat(store.get().value.getValue("BTC").readings).isNotEmpty()
    }

    @Test
    fun `GIVEN symbols and indicators WHEN fetchCoinIndicators THEN request params are comma-joined and lowercased`() =
        runTest {
            // Arrange
            coEvery { marketsApi.getCoinIndicators(any(), any()) } returns success()

            // Act
            repository.fetchCoinIndicators(
                symbols = listOf("BTC", "ETH"),
                indicators = listOf(CoinIndicators.Reading.Type.RSI, CoinIndicators.Reading.Type.MA_CROSS),
            )

            // Assert
            coVerify(exactly = 1) {
                marketsApi.getCoinIndicators(symbols = "BTC,ETH", indicators = "rsi,ma_cross")
            }
        }

    @Test
    fun `GIVEN empty symbols WHEN fetchCoinIndicators THEN symbols param is null`() = runTest {
        // Arrange — an empty list means "all coins", which the wire represents as an absent param
        coEvery { marketsApi.getCoinIndicators(any(), any()) } returns success()

        // Act
        repository.fetchCoinIndicators(symbols = emptyList(), indicators = null)

        // Assert
        coVerify(exactly = 1) { marketsApi.getCoinIndicators(symbols = null, indicators = null) }
    }

    @Test
    fun `GIVEN api throws WHEN fetchCoinIndicators THEN returns Left and store is untouched`() = runTest {
        // Arrange
        store.store(mapOf("ETH" to coinIndicators("ETH")))
        val failure = RuntimeException("api down")
        coEvery { marketsApi.getCoinIndicators(any(), any()) } throws failure

        // Act
        val result = repository.fetchCoinIndicators(symbols = listOf("BTC"), indicators = null)

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(store.get().value.keys).containsExactly("ETH")
    }

    @Test
    fun `GIVEN a populated response WHEN getMarketIndicators THEN returns the converted indicators`() = runTest {
        // Arrange
        coEvery { marketsApi.getMarketIndicators(any(), any()) } returns ApiResponse.Success(
            MarketIndicatorsResponse(
                totalMarketCap = BigDecimal("2679300000000"),
                marketCapChangePercentage24h = BigDecimal("-0.93"),
                fearGreedValue = 72,
                fearGreedClassification = "Greed",
                altcoinIndexValue = 44,
                altcoinIndexClassification = "Balanced Market",
            ),
        )

        // Act
        val actual = repository.getMarketIndicators(fiatCurrencyCode = "USD", languageCode = "en")

        // Assert
        val indicators = actual.getOrNull()!!
        assertThat(indicators.totalMarketCap).isEqualToIgnoringScale("2679300000000")
        assertThat(indicators.fearGreed).isEqualTo(MarketIndicators.Index(value = 72, classification = "Greed"))
        coVerify(exactly = 1) { marketsApi.getMarketIndicators(any(), any()) }
    }

    @Test
    fun `GIVEN an all-null response WHEN getMarketIndicators THEN returns Right with empty indicators`() = runTest {
        // Arrange — a response without data is not a failure
        coEvery { marketsApi.getMarketIndicators(any(), any()) } returns ApiResponse.Success(MarketIndicatorsResponse())

        // Act
        val actual = repository.getMarketIndicators(fiatCurrencyCode = "USD", languageCode = "en")

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(
            MarketIndicators(
                totalMarketCap = null,
                marketCapChange24h = null,
                fearGreed = null,
                altcoinSeason = null,
            ),
        )
    }

    @Test
    fun `GIVEN the api throws WHEN getMarketIndicators THEN returns Left`() = runTest {
        // Arrange
        coEvery { marketsApi.getMarketIndicators(any(), any()) } throws RuntimeException("api down")

        // Act
        val actual = repository.getMarketIndicators(fiatCurrencyCode = "USD", languageCode = "en")

        // Assert
        assertThat(actual.isLeft()).isTrue()
    }

    private fun success(vararg assets: Asset): ApiResponse<GetCoinIndicatorsResponse> =
        ApiResponse.Success(GetCoinIndicatorsResponse(assets = assets.toList()))

    private fun asset(symbol: String) = Asset(
        symbol = symbol,
        indicators = listOf(
            Indicator(
                type = Indicator.Type.RSI,
                name = "RSI",
                timeframe = Indicator.Timeframe.H24,
                value = null,
                label = Indicator.Signal.POSITIVE,
                updatedAt = null,
            ),
        ),
    )

    private fun coinIndicators(symbol: String, readingCount: Int = 1) = CoinIndicators(
        symbol = symbol,
        readings = List(readingCount) {
            CoinIndicators.Reading(
                type = CoinIndicators.Reading.Type.RSI,
                name = "RSI",
                timeframe = CoinIndicators.Reading.Timeframe.DAY,
                value = null,
                signal = CoinIndicators.Reading.Signal.NEUTRAL,
                updatedAt = null,
            )
        },
    )
}