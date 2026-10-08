package com.tangem.features.yield.supply.impl.chart.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData
import com.tangem.domain.yield.supply.models.incentives.YieldAprHistory
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.domain.yield.supply.usecase.GetYieldAprHistoryUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetChartUseCase
import com.tangem.features.yield.supply.impl.chart.DefaultYieldSupplyChartComponent
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartPeriod
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartUM
import com.tangem.test.mock.yield.MockYieldSupply
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyChartModelTest {

    private val getChartUseCase: YieldSupplyGetChartUseCase = mockk()
    private val getTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase = mockk()
    private val getAprHistoryUseCase: GetYieldAprHistoryUseCase = mockk()
    private val callback: DefaultYieldSupplyChartComponent.ModelCallback = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        clearMocks(getChartUseCase, getTokenStatusUseCase, getAprHistoryUseCase, callback)
        coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = false).right()
    }

    @Test
    fun `GIVEN chart data with values above one WHEN model created THEN Data state with integer percent format`() =
        runTest {
            // Arrange
            coEvery { getChartUseCase(any()) } returns chartData(y = listOf(2.0, 5.0, 10.0)).right()

            // Act
            val model = createModel()

            // Assert
            val state = model.uiState.value
            assertThat(state).isInstanceOf(YieldSupplyChartUM.Data::class.java)
            val data = state as YieldSupplyChartUM.Data
            assertThat(data.chartData.percentFormat).isEqualTo("%.0f")
            assertThat(data.monthLables).hasSize(MONTH_LABELS_COUNT)
            verify(exactly = 1) { callback.onStartLoading() }
            verify(exactly = 1) { callback.onSuccessLoad() }
            verify(exactly = 0) { callback.onLoadFail() }
        }

    @Test
    fun `GIVEN chart data with values below one WHEN model created THEN Data state with one-decimal percent format`() =
        runTest {
            // Arrange
            coEvery { getChartUseCase(any()) } returns chartData(y = listOf(0.2, 0.5, 0.9)).right()

            // Act
            val model = createModel()

            // Assert
            val data = model.uiState.value as YieldSupplyChartUM.Data
            assertThat(data.chartData.percentFormat).isEqualTo("%.1f")
        }

    @Test
    fun `GIVEN chart data below a tenth WHEN model created THEN Data state with two-decimal percent format`() =
        runTest {
            // Arrange
            coEvery { getChartUseCase(any()) } returns chartData(y = listOf(0.02, 0.05, 0.09)).right()

            // Act
            val model = createModel()

            // Assert
            val data = model.uiState.value as YieldSupplyChartUM.Data
            assertThat(data.chartData.percentFormat).isEqualTo("%.2f")
        }

    @Test
    fun `GIVEN empty chart data WHEN model created THEN Error state and load fail callback`() = runTest {
        // Arrange
        coEvery { getChartUseCase(any()) } returns chartData(y = emptyList()).right()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(YieldSupplyChartUM.Error::class.java)
        verify(exactly = 1) { callback.onStartLoading() }
        verify(exactly = 1) { callback.onLoadFail() }
        verify(exactly = 0) { callback.onSuccessLoad() }
    }

    @Test
    fun `GIVEN use case fails WHEN model created THEN Error state and load fail callback`() = runTest {
        // Arrange
        coEvery { getChartUseCase(any()) } returns IllegalStateException("boom").left()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(YieldSupplyChartUM.Error::class.java)
        verify(exactly = 1) { callback.onLoadFail() }
        verify(exactly = 0) { callback.onSuccessLoad() }
    }

    @Test
    fun `GIVEN error state WHEN retry invoked AND data available THEN recovers to Data state`() = runTest {
        // Arrange — first call fails, retry succeeds
        coEvery { getChartUseCase(any()) } returnsMany listOf(
            IllegalStateException("boom").left(),
            chartData(y = listOf(2.0, 5.0)).right(),
        )
        val model = createModel()
        val error = model.uiState.value as YieldSupplyChartUM.Error

        // Act
        error.onRetry()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(YieldSupplyChartUM.Data::class.java)
    }

    @Test
    fun `GIVEN no callback WHEN model created with data THEN Data state without crash`() = runTest {
        // Arrange — Params.callback is optional; model must tolerate its absence
        coEvery { getChartUseCase(any()) } returns chartData(y = listOf(2.0, 5.0)).right()

        // Act
        val model = createModel(callback = null)

        // Assert
        assertThat(model.uiState.value).isInstanceOf(YieldSupplyChartUM.Data::class.java)
    }

    @Test
    fun `GIVEN active campaign WHEN model created THEN incentivised points and their mean are shown`() = runTest {
        // Arrange
        coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = true).right()
        coEvery { getAprHistoryUseCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = 7) } returns
            aprHistory("4.0", "5.0", "6.0").right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val data = model.uiState.value as YieldSupplyChartUM.Data
        assertThat(data.chartData.y).containsExactly(2.5, 3.5, 4.5).inOrder()
        assertThat(data.chartData.rewardCaps).containsExactly(1.5, 1.5, 1.5).inOrder()
        assertThat(data.chartData.avr).isEqualTo(5.0)
        val incentive = requireNotNull(data.incentive)
        assertThat(incentive.period).isEqualTo(YieldSupplyChartPeriod.Week)
        // Already formatted: the header prints it as it stands, so a raw mean would leak the payload's scale
        assertThat(incentive.averageApy).isEqualTo("5.00%")
    }

    @Test
    fun `GIVEN rewards apr out of step with the total WHEN model created THEN the bar ends at the total`() =
        runTest {
            // Arrange — MTR-1 forbids composing the rate locally: the bar is the backend total, and the cap is
            // only what the total leaves above the base. The reported rewards APR is not a third opinion.
            coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = true).right()
            coEvery { getAprHistoryUseCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = 7) } returns
                YieldAprHistory(
                    chainId = CHAIN_ID,
                    tokenAddress = TOKEN_ADDRESS,
                    points = listOf(point(totalApyEst = "6.0", baseApy = "4.0", rewardsApr = "1.0")),
                ).right()

            // Act
            val model = createModel()
            advanceUntilIdle()

            // Assert
            val data = (model.uiState.value as YieldSupplyChartUM.Data).chartData
            assertThat(data.y).containsExactly(4.0)
            assertThat(data.rewardCaps).containsExactly(2.0)
        }

    @Test
    fun `GIVEN total below base WHEN model created THEN the cap is zero`() = runTest {
        // Arrange — a total under the base must not invert the bar into a negative cap
        coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = true).right()
        coEvery { getAprHistoryUseCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = 7) } returns
            YieldAprHistory(
                chainId = CHAIN_ID,
                tokenAddress = TOKEN_ADDRESS,
                points = listOf(point(totalApyEst = "3.5", baseApy = "4.0", rewardsApr = "1.0")),
            ).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val data = (model.uiState.value as YieldSupplyChartUM.Data).chartData
        assertThat(data.y).containsExactly(3.5)
        assertThat(data.rewardCaps).containsExactly(0.0)
    }

    @Test
    fun `GIVEN active campaign WHEN three months selected THEN history reloads for ninety days`() = runTest {
        // Arrange
        coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = true).right()
        coEvery { getAprHistoryUseCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = 7) } returns
            aprHistory("4.0").right()
        coEvery { getAprHistoryUseCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = 90) } returns
            aprHistory("7.0", "9.0").right()

        // Act
        val model = createModel()
        advanceUntilIdle()
        (model.uiState.value as YieldSupplyChartUM.Data).incentive!!
            .onPeriodSelected(YieldSupplyChartPeriod.ThreeMonths.name)
        advanceUntilIdle()

        // Assert
        val data = model.uiState.value as YieldSupplyChartUM.Data
        assertThat(data.chartData.y).containsExactly(5.5, 7.5).inOrder()
        assertThat(data.incentive!!.period).isEqualTo(YieldSupplyChartPeriod.ThreeMonths)
    }

    @Test
    fun `GIVEN carried forward points WHEN model created THEN they are kept`() = runTest {
        // Arrange
        coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = true).right()
        coEvery { getAprHistoryUseCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = 7) } returns
            YieldAprHistory(
                chainId = CHAIN_ID,
                tokenAddress = TOKEN_ADDRESS,
                points = listOf(
                    point("4.0"),
                    point("5.0", isCarriedForward = true),
                    point("6.0"),
                ),
            ).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val data = model.uiState.value as YieldSupplyChartUM.Data
        assertThat(data.chartData.y).containsExactly(2.5, 3.5, 4.5).inOrder()
    }

    @Test
    fun `GIVEN a week of history WHEN model created THEN every bar carries a weekday label`() = runTest {
        // Arrange
        coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = true).right()
        coEvery { getAprHistoryUseCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = 7) } returns
            aprHistory("4.0", "5.0", "6.0").right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val labels = (model.uiState.value as YieldSupplyChartUM.Data).chartData.xLabels
        assertThat(labels).hasSize(3)
        assertThat(labels.none(String::isBlank)).isTrue()
    }

    @Test
    fun `GIVEN three months of history WHEN model created THEN only the first bar of a month is labelled`() =
        runTest {
            // Arrange
            coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = true).right()
            coEvery { getAprHistoryUseCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = 7) } returns
                aprHistory("4.0").right()
            coEvery { getAprHistoryUseCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = 90) } returns
                YieldAprHistory(
                    chainId = CHAIN_ID,
                    tokenAddress = TOKEN_ADDRESS,
                    points = listOf(
                        point("4.0", date = LocalDate(year = 2026, monthNumber = 9, dayOfMonth = 29)),
                        point("5.0", date = LocalDate(year = 2026, monthNumber = 9, dayOfMonth = 30)),
                        point("6.0", date = LocalDate(year = 2026, monthNumber = 10, dayOfMonth = 1)),
                    ),
                ).right()

            // Act
            val model = createModel()
            advanceUntilIdle()
            (model.uiState.value as YieldSupplyChartUM.Data).incentive!!
                .onPeriodSelected(YieldSupplyChartPeriod.ThreeMonths.name)
            advanceUntilIdle()

            // Assert
            val labels = (model.uiState.value as YieldSupplyChartUM.Data).chartData.xLabels
            assertThat(labels[0]).isNotEmpty()
            assertThat(labels[1]).isEmpty()
            assertThat(labels[2]).isNotEmpty()
        }

    @Test
    fun `GIVEN empty incentivised history WHEN model created THEN error state`() = runTest {
        // Arrange
        coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = true).right()
        coEvery { getAprHistoryUseCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = 7) } returns
            YieldAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, points = emptyList()).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(YieldSupplyChartUM.Error::class.java)
        verify(exactly = 1) { callback.onLoadFail() }
    }

    @Test
    fun `GIVEN incentives unavailable WHEN model created THEN error state and legacy chart is not used`() = runTest {
        // Arrange
        coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = true).right()
        coEvery { getAprHistoryUseCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = 7) } returns
            YieldIncentivesError.Unavailable.left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(YieldSupplyChartUM.Error::class.java)
        coVerify(exactly = 0) { getChartUseCase(any()) }
    }

    @Test
    fun `GIVEN campaign is over WHEN model created THEN the legacy chart is used`() = runTest {
        // Arrange
        coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = false).right()
        coEvery { getChartUseCase(any()) } returns chartData(y = listOf(2.0, 5.0)).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val data = model.uiState.value as YieldSupplyChartUM.Data
        assertThat(data.incentive).isNull()
        coVerify(exactly = 0) { getAprHistoryUseCase(any(), any(), any()) }
    }

    private fun aprHistory(vararg totalApyEst: String) = YieldAprHistory(
        chainId = CHAIN_ID,
        tokenAddress = TOKEN_ADDRESS,
        points = totalApyEst.mapIndexed { index, total -> point(total, date = SEPTEMBER_FIRST.plus(index, DAY)) },
    )

    private fun point(
        totalApyEst: String,
        isCarriedForward: Boolean = false,
        date: LocalDate = SEPTEMBER_FIRST,
        baseApy: String? = null,
        rewardsApr: String = REWARDS_APR.toPlainString(),
    ): YieldAprHistory.Point {
        val total = BigDecimal(totalApyEst)

        return YieldAprHistory.Point(
            date = date,
            baseApy = baseApy?.let(::BigDecimal) ?: (total - REWARDS_APR),
            rewardsApr = BigDecimal(rewardsApr),
            totalApyEst = total,
            isCarriedForward = isCarriedForward,
        )
    }

    private fun market(hasCampaign: Boolean) = MockYieldSupply.market(
        tokenAddress = TOKEN_ADDRESS,
        chainId = CHAIN_ID,
        apy = BigDecimal("4.0"),
        campaign = MockYieldSupply.campaign(hasActiveCampaign = hasCampaign),
    )

    @Test
    fun `GIVEN active campaign WHEN the plain rate chart is asked for THEN the Aave history is shown`() = runTest {
        // Arrange - the variable-rate pill opens its own sheet, which must not turn into the incentivised one
        coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = true).right()
        coEvery { getChartUseCase(any()) } returns chartData(y = listOf(2.0, 5.0, 10.0)).right()

        // Act
        val model = createModel(isPlainRate = true)
        advanceUntilIdle()

        // Assert
        val data = model.uiState.value as YieldSupplyChartUM.Data
        assertThat(data.chartData.rewardCaps).isEmpty()
        assertThat(data.incentive).isNull()
        verify(exactly = 1) { callback.onIncentiveChanged(null) }
        coVerify(exactly = 0) { getAprHistoryUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN three months as the initial period WHEN model created THEN that history is asked for`() = runTest {
        // Arrange - the inline chart on the active screen has no picker, so it must open on the period it is given
        coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = true).right()
        coEvery { getAprHistoryUseCase(any(), any(), any()) } returns aprHistory("4.0", "5.0", "6.0").right()

        // Act
        val model = createModel(initialPeriod = YieldSupplyChartPeriod.ThreeMonths)
        advanceUntilIdle()

        // Assert
        val data = model.uiState.value as YieldSupplyChartUM.Data
        assertThat(data.incentive?.period).isEqualTo(YieldSupplyChartPeriod.ThreeMonths)
        coVerify(exactly = 1) {
            getAprHistoryUseCase(
                chainId = CHAIN_ID,
                tokenAddress = TOKEN_ADDRESS,
                days = YieldSupplyChartPeriod.ThreeMonths.days,
            )
        }
    }

    private fun createModel(
        callback: DefaultYieldSupplyChartComponent.ModelCallback? = this.callback,
        isPlainRate: Boolean = false,
        initialPeriod: YieldSupplyChartPeriod = YieldSupplyChartPeriod.Week,
    ): YieldSupplyChartModel = YieldSupplyChartModel(
        paramsContainer = MutableParamsContainer(
            DefaultYieldSupplyChartComponent.Params(
                cryptoCurrency = createToken(),
                isPlainRate = isPlainRate,
                initialPeriod = initialPeriod,
                callback = callback,
            ),
        ),
        dispatchers = TestingCoroutineDispatcherProvider(),
        yieldSupplyGetChartUseCase = getChartUseCase,
        yieldSupplyGetTokenStatusUseCase = getTokenStatusUseCase,
        getYieldAprHistoryUseCase = getAprHistoryUseCase,
    )

    private fun chartData(y: List<Double>): YieldSupplyMarketChartData =
        YieldSupplyMarketChartData(y = y, x = y.indices.map { it.toDouble() }, avr = 1.0)

    private fun createToken() = MockYieldSupply.token(contractAddress = TOKEN_ADDRESS)

    private companion object {
        const val MONTH_LABELS_COUNT = 5
        const val CHAIN_ID = 1
        const val TOKEN_ADDRESS = "0xToken"
        val REWARDS_APR: BigDecimal = BigDecimal("1.5")
        val SEPTEMBER_FIRST = LocalDate(year = 2026, monthNumber = 9, dayOfMonth = 1)
        val DAY = DateTimeUnit.DAY
    }
}