package com.tangem.features.send.feeselector.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.subcomponents.feeSelector.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeExtraInfo
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams
import com.tangem.features.send.feeselector.route.FeeSelectorRoute
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeeSelectorModelTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN selector options WHEN initial route requested THEN opens the matching screen`(model: InitialRouteModel) {
        // Arrange
        val feeSelectorLogic: FeeSelectorLogic = mockk {
            every { isGaslessEnabled } returns model.isGaslessEnabled
            every { uiState } returns MutableStateFlow(FeeSelectorUM.Loading)
        }
        val feeSelectorModel = FeeSelectorModel(
            paramsContainer = MutableParamsContainer(
                value = mockk<FeeSelectorParams.FeeSelectorDetailsParams>(relaxed = true) {
                    every { shouldShowOnlySpeedOption } returns model.shouldShowOnlySpeedOption
                    every { shouldShowOnlyTokenOption } returns model.shouldShowOnlyTokenOption
                },
            ),
            feeSelectorLogicFactory = mockk { every { create(any(), any()) } returns feeSelectorLogic },
            dispatchers = TestingCoroutineDispatcherProvider(),
            analyticsEventHandler = mockk(relaxed = true),
        )

        // Act
        val route = feeSelectorModel.getInitialRoute()

        // Assert
        assertThat(route).isEqualTo(model.expected)
        feeSelectorModel.onDestroy()
    }

    private fun provideTestModels() = listOf(
        InitialRouteModel(isGaslessEnabled = true, expected = FeeSelectorRoute.NetworkFee),
        InitialRouteModel(isGaslessEnabled = false, expected = FeeSelectorRoute.ChooseSpeed),
        InitialRouteModel(
            isGaslessEnabled = true,
            shouldShowOnlyTokenOption = true,
            expected = FeeSelectorRoute.ChooseToken,
        ),
        InitialRouteModel(
            isGaslessEnabled = false,
            shouldShowOnlyTokenOption = true,
            expected = FeeSelectorRoute.ChooseSpeed,
        ),
        InitialRouteModel(
            isGaslessEnabled = true,
            shouldShowOnlySpeedOption = true,
            shouldShowOnlyTokenOption = true,
            expected = FeeSelectorRoute.ChooseSpeed,
        ),
    )

    @Nested
    inner class OnTokenSelected {

        @Test
        fun `GIVEN token-only list WHEN token selected THEN pick returned without a reload`() {
            // Arrange
            val callback: FeeSelectorModelCallback = mockk(relaxed = true)
            val feeSelectorLogic = createLogic(state = MutableStateFlow(content(feeCurrency = nativeStatus)))
            val feeSelectorModel = createModel(feeSelectorLogic, callback = callback, shouldShowOnlyTokenOption = true)
            val expected = content(feeCurrency = nativeStatus).copy(
                isPrimaryButtonEnabled = false,
                selectedFeeItem = FeeItem.Loading,
                feeItems = persistentListOf(FeeItem.Loading),
                feeExtraInfo = extraInfo(feeCurrency = tokenStatus).copy(isFeeTokenSelectedByUser = true),
            )

            // Act
            feeSelectorModel.onTokenSelected(tokenStatus)

            // Assert
            verify(exactly = 1) { callback.onFeeResult(expected) }
            verify(exactly = 0) { feeSelectorLogic.onTokenSelected(any()) }
            feeSelectorModel.onDestroy()
        }

        @Test
        fun `GIVEN full fee selector WHEN token selected THEN fee reloaded in the token`() {
            // Arrange
            val callback: FeeSelectorModelCallback = mockk(relaxed = true)
            val feeSelectorLogic = createLogic(state = MutableStateFlow(content(feeCurrency = nativeStatus)))
            val feeSelectorModel = createModel(feeSelectorLogic, callback = callback, shouldShowOnlyTokenOption = false)

            // Act
            feeSelectorModel.onTokenSelected(tokenStatus)

            // Assert
            verify(exactly = 1) { feeSelectorLogic.onTokenSelected(tokenStatus) }
            verify(exactly = 0) { callback.onFeeResult(any()) }
            feeSelectorModel.onDestroy()
        }
    }

    @Nested
    inner class OnChooseTokenScreenOpened {

        @Test
        fun `GIVEN list still loading WHEN screen opened THEN event waits for the loaded tokens`() {
            // Arrange
            val state = MutableStateFlow<FeeSelectorUM>(FeeSelectorUM.Loading)
            val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
            val feeSelectorModel = createModel(createLogic(state), analyticsEventHandler = analyticsEventHandler)

            // Act
            feeSelectorModel.onChooseTokenScreenOpened()
            state.value = content(feeCurrency = nativeStatus, available = listOf(nativeStatus, tokenStatus))

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(any()) }
            verify(exactly = 1) { analyticsEventHandler.send(expectedTokenScreenEvent) }
            feeSelectorModel.onDestroy()
        }

        @Test
        fun `GIVEN list loaded WHEN screen opened THEN event sent at once`() {
            // Arrange
            val state = MutableStateFlow<FeeSelectorUM>(
                content(feeCurrency = nativeStatus, available = listOf(nativeStatus, tokenStatus)),
            )
            val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
            val feeSelectorModel = createModel(createLogic(state), analyticsEventHandler = analyticsEventHandler)

            // Act
            feeSelectorModel.onChooseTokenScreenOpened()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(expectedTokenScreenEvent) }
            feeSelectorModel.onDestroy()
        }

        private val expectedTokenScreenEvent = CommonSendAnalyticEvents.FeeTokenScreenOpened(
            categoryName = "Earning",
            source = CommonSendAnalyticEvents.CommonSendSource.Earning,
            availableTokens = "ETH, USDC",
            blockchain = "Ethereum",
        )
    }

    private val nativeStatus = currencyStatus(symbol = "ETH")
    private val tokenStatus = currencyStatus(symbol = "USDC")

    private fun currencyStatus(symbol: String): CryptoCurrencyStatus = mockk(relaxed = true) {
        every { currency.symbol } returns symbol
    }

    private fun createLogic(state: MutableStateFlow<FeeSelectorUM>): FeeSelectorLogic = mockk {
        every { uiState } returns state
        justRun { onTokenSelected(any()) }
    }

    private fun createModel(
        feeSelectorLogic: FeeSelectorLogic,
        callback: FeeSelectorModelCallback = mockk(relaxed = true),
        analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true),
        shouldShowOnlyTokenOption: Boolean = true,
    ) = FeeSelectorModel(
        paramsContainer = MutableParamsContainer(
            value = mockk<FeeSelectorParams.FeeSelectorDetailsParams>(relaxed = true) {
                every { this@mockk.callback } returns callback
                every { this@mockk.shouldShowOnlyTokenOption } returns shouldShowOnlyTokenOption
                every { analyticsCategoryName } returns "Earning"
                every { analyticsSendSource } returns CommonSendAnalyticEvents.CommonSendSource.Earning
                every { cryptoCurrencyStatus.currency.network.name } returns "Ethereum"
            },
        ),
        feeSelectorLogicFactory = mockk { every { create(any(), any()) } returns feeSelectorLogic },
        dispatchers = TestingCoroutineDispatcherProvider(),
        analyticsEventHandler = analyticsEventHandler,
    )

    private fun extraInfo(
        feeCurrency: CryptoCurrencyStatus,
        available: List<CryptoCurrencyStatus>? = null,
    ) = FeeExtraInfo(
        isFeeApproximate = false,
        isFeeConvertibleToFiat = true,
        isTronToken = false,
        feeCryptoCurrencyStatus = feeCurrency,
        availableFeeCurrencies = available?.let { persistentListOf(*it.toTypedArray()) },
    )

    private fun content(
        feeCurrency: CryptoCurrencyStatus,
        available: List<CryptoCurrencyStatus>? = null,
    ): FeeSelectorUM.Content {
        val fee = Fee.Common(Amount(currencySymbol = "ETH", value = BigDecimal("0.001"), decimals = 18))
        return FeeSelectorUM.Content(
            isPrimaryButtonEnabled = true,
            fees = TransactionFee.Single(normal = fee),
            feeItems = persistentListOf(FeeItem.Loading),
            selectedFeeItem = FeeItem.Loading,
            feeExtraInfo = extraInfo(feeCurrency = feeCurrency, available = available),
            feeFiatRateUM = null,
            feeNonce = FeeNonce.None,
        )
    }

    data class InitialRouteModel(
        val isGaslessEnabled: Boolean,
        val shouldShowOnlySpeedOption: Boolean = false,
        val shouldShowOnlyTokenOption: Boolean = false,
        val expected: FeeSelectorRoute,
    )
}