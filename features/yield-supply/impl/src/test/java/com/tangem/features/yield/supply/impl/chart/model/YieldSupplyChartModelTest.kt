package com.tangem.features.yield.supply.impl.chart.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetChartUseCase
import com.tangem.features.yield.supply.impl.chart.DefaultYieldSupplyChartComponent
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyChartModelTest {

    private val getChartUseCase: YieldSupplyGetChartUseCase = mockk()
    private val callback: DefaultYieldSupplyChartComponent.ModelCallback = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        clearMocks(getChartUseCase, callback)
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

    private fun createModel(
        callback: DefaultYieldSupplyChartComponent.ModelCallback? = this.callback,
    ): YieldSupplyChartModel = YieldSupplyChartModel(
        paramsContainer = MutableParamsContainer(
            DefaultYieldSupplyChartComponent.Params(cryptoCurrency = createToken(), callback = callback),
        ),
        dispatchers = TestingCoroutineDispatcherProvider(),
        yieldSupplyGetChartUseCase = getChartUseCase,
    )

    private fun chartData(y: List<Double>): YieldSupplyMarketChartData =
        YieldSupplyMarketChartData(y = y, x = y.indices.map { it.toDouble() }, avr = 1.0)

    private fun createToken(): CryptoCurrency.Token {
        val derivationPath = Network.DerivationPath.None
        val network = Network(
            id = Network.ID(value = "ethereum", derivationPath = derivationPath),
            name = "Ethereum",
            currencySymbol = "ETH",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
                suffix = CryptoCurrency.ID.Suffix.RawID("ethereum"),
            ),
            network = network,
            name = "TEST_TOKEN",
            symbol = "TTK",
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xToken",
        )
    }

    private companion object {
        const val MONTH_LABELS_COUNT = 5
    }
}