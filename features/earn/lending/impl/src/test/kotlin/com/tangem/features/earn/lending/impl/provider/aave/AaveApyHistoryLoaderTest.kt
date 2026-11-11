package com.tangem.features.earn.lending.impl.provider.aave

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldAprHistory
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.domain.yield.supply.usecase.GetYieldAprHistoryUseCase
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnApyPoint
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.test.core.ProvideTestModels
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
    private val asset = LendingAsset(
        currency = currencies.createCoin(Blockchain.Ethereum),
        wrappedToken = currencies.createToken(Blockchain.Ethereum, contractAddress = WETH, symbol = "WETH"),
    )
    private val market = YieldMarketToken(
        tokenAddress = WETH,
        chainId = CHAIN_ID,
        apy = BigDecimal("3"),
        isActive = true,
        maxFeeNative = BigDecimal.ONE,
        maxFeeUSD = BigDecimal.ONE,
    )

    private val markets: AaveMarkets = mockk()
    private val getAprHistory: GetYieldAprHistoryUseCase = mockk()
    private val loader = AaveApyHistoryLoader(markets = markets, getAprHistory = getAprHistory)

    @BeforeEach
    fun resetMocks() {
        clearMocks(markets, getAprHistory)
        coEvery { markets.current(asset) } returns market
        coEvery { getAprHistory(any(), any(), any()) } returns emptyHistory().right()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN period WHEN load THEN history is requested for its depth in days`(model: PeriodModel) = runTest {
        // Act
        loader.load(asset, model.period)

        // Assert
        coVerify(exactly = 1) { getAprHistory(chainId = CHAIN_ID, tokenAddress = WETH, days = model.expectedDays) }
    }

    data class PeriodModel(val period: EarnPeriod, val expectedDays: Int)

    private fun provideTestModels() = listOf(
        PeriodModel(period = AaveApyPeriods.Month, expectedDays = 30),
        PeriodModel(period = AaveApyPeriods.ThreeMonths, expectedDays = 90),
    )

    @Test
    fun `GIVEN points with rewards WHEN load THEN rates come from total APY estimate`() = runTest {
        // Arrange
        val history = emptyHistory().copy(
            points = listOf(point(day = 1, totalApyEst = "5.5"), point(day = 2, totalApyEst = "4.5")),
        )
        coEvery { getAprHistory(any(), any(), any()) } returns history.right()

        // Act
        val actual = loader.load(asset, AaveApyPeriods.Month)

        // Assert
        val expected = EarnApyHistory(
            points = listOf(
                EarnApyPoint(at = Instant.parse("2026-11-01T00:00:00Z"), rate = apy("0.055")),
                EarnApyPoint(at = Instant.parse("2026-11-02T00:00:00Z"), rate = apy("0.045")),
            ),
            average = BigDecimal("0.050"),
        )
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN no WETH market WHEN load THEN empty history without request`() = runTest {
        // Arrange
        coEvery { markets.current(asset) } returns null

        // Act
        val actual = loader.load(asset, AaveApyPeriods.Month)

        // Assert
        assertThat(actual).isEqualTo(EarnApyHistory(points = emptyList(), average = null).right())
        coVerify(exactly = 0) { getAprHistory(any(), any(), any()) }
    }

    @Test
    fun `GIVEN request fails WHEN load THEN Left`() = runTest {
        // Arrange
        coEvery { getAprHistory(any(), any(), any()) } returns YieldIncentivesError.FeatureAbsent.left()

        // Act
        val actual = loader.load(asset, AaveApyPeriods.Month)

        // Assert
        assertThat(actual.isLeft()).isTrue()
    }

    private fun emptyHistory() = YieldAprHistory(chainId = CHAIN_ID, tokenAddress = WETH, points = emptyList())

    private fun point(day: Int, totalApyEst: String) = YieldAprHistory.Point(
        date = LocalDate(year = 2026, monthNumber = 11, dayOfMonth = day),
        baseApy = BigDecimal("3"),
        rewardsApy = BigDecimal("1"),
        totalApyEst = BigDecimal(totalApyEst),
        isCarriedForward = false,
    )

    private fun apy(fraction: String) = EarnProductRate(fraction = BigDecimal(fraction), type = EarnRateType.APY)

    private companion object {
        const val CHAIN_ID = 1
        const val WETH = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
    }
}