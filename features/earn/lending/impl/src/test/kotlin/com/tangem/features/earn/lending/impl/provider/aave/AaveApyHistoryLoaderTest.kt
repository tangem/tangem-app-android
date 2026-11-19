package com.tangem.features.earn.lending.impl.provider.aave

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.yield.supply.models.YieldChartWindow
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetChartUseCase
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnApyPoint
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.assertEitherLeft
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AaveApyHistoryLoaderTest {

    private val currencies = MockCryptoCurrencyFactory()
    private val weth = currencies.createToken(Blockchain.Ethereum, contractAddress = WETH, symbol = "WETH")
    private val asset = LendingAsset(currency = currencies.createCoin(Blockchain.Ethereum), wrappedToken = weth)

    private val getChart: YieldSupplyGetChartUseCase = mockk()
    private val loader = AaveApyHistoryLoader(getChart = getChart)

    @BeforeEach
    fun resetMocks() {
        clearMocks(getChart)
        coEvery { getChart(cryptoCurrency = any(), window = any(), bucketSizeDays = any()) } returns
            chart(y = emptyList(), starts = emptyList()).right()
    }

    @Test
    fun `GIVEN periods WHEN listed THEN 1M, 3M and 6M`() {
        // Act
        val actual = AaveApyPeriods.All.map(EarnPeriod::id)

        // Assert
        assertThat(actual).containsExactly("1M", "3M", "6M").inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN period WHEN load THEN the WETH chart is requested for its window and bar size`(model: PeriodModel) =
        runTest {
            // Act
            loader.load(asset, period(model.id))

            // Assert
            coVerify(exactly = 1) {
                getChart(cryptoCurrency = weth, window = model.window, bucketSizeDays = model.bucketSizeDays)
            }
        }

    private fun provideTestModels() = listOf(
        PeriodModel(id = "1M", window = YieldChartWindow.LastMonth, bucketSizeDays = 1),
        PeriodModel(id = "3M", window = YieldChartWindow.LastYear, bucketSizeDays = 3),
        PeriodModel(id = "6M", window = YieldChartWindow.LastYear, bucketSizeDays = 6),
    )

    @Test
    fun `GIVEN month chart WHEN load THEN every bar with the backend average`() = runTest {
        // Arrange
        coEvery { getChart(cryptoCurrency = any(), window = any(), bucketSizeDays = any()) } returns chart(
            y = listOf(2.0, 4.0),
            starts = listOf(LocalDate(2026, 11, 1), LocalDate(2026, 11, 2)),
            avr = 3.5,
        ).right()

        // Act
        val actual = loader.load(asset, period("1M"))

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(
            EarnApyHistory(
                points = listOf(
                    point(at = "2026-11-01T00:00:00Z", rate = "0.02"),
                    point(at = "2026-11-02T00:00:00Z", rate = "0.04"),
                ),
                average = BigDecimal("0.035"),
            ),
        )
    }

    @Test
    fun `GIVEN year chart WHEN load 3M THEN only the last three months and their mean`() = runTest {
        // Arrange
        coEvery { getChart(cryptoCurrency = any(), window = any(), bucketSizeDays = any()) } returns chart(
            y = listOf(10.0, 2.0, 4.0),
            starts = listOf(LocalDate(2026, 5, 1), LocalDate(2026, 9, 1), LocalDate(2026, 11, 1)),
            avr = 9.0,
        ).right()

        // Act
        val actual = loader.load(asset, period("3M"))

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(
            EarnApyHistory(
                points = listOf(
                    point(at = "2026-09-01T00:00:00Z", rate = "0.02"),
                    point(at = "2026-11-01T00:00:00Z", rate = "0.04"),
                ),
                average = BigDecimal("0.03"),
            ),
        )
    }

    @Test
    fun `GIVEN bars without dates WHEN load THEN empty history`() = runTest {
        // Arrange
        coEvery { getChart(cryptoCurrency = any(), window = any(), bucketSizeDays = any()) } returns
            chart(y = listOf(2.0), starts = emptyList()).right()

        // Act
        val actual = loader.load(asset, period("1M"))

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(EarnApyHistory(points = emptyList(), average = null))
    }

    @Test
    fun `GIVEN request fails WHEN load THEN Left`() = runTest {
        // Arrange
        val error = IllegalStateException("backend")
        coEvery { getChart(cryptoCurrency = any(), window = any(), bucketSizeDays = any()) } returns error.left()

        // Act
        val actual = loader.load(asset, period("1M"))

        // Assert
        assertEitherLeft(actual, error)
    }

    data class PeriodModel(val id: String, val window: YieldChartWindow, val bucketSizeDays: Int)

    private fun period(id: String) = AaveApyPeriods.All.single { it.id == id }

    private fun chart(y: List<Double>, starts: List<LocalDate>, avr: Double = 0.0) = YieldSupplyMarketChartData(
        y = y,
        x = y.indices.map(Int::toDouble),
        avr = avr,
        bucketStarts = starts,
    )

    private fun point(at: String, rate: String) = EarnApyPoint(
        at = Instant.parse(at),
        rate = EarnProductRate(fraction = BigDecimal(rate), type = EarnRateType.APY),
    )

    private companion object {
        const val WETH = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
    }
}