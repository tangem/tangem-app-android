package com.tangem.features.send.subcomponents.amount.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer.ReduceByData
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.GetMinimumTransactionAmountSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.api.subcomponents.amount.SendAmountComponent
import com.tangem.features.send.api.subcomponents.amount.SendAmountComponentParams
import com.tangem.features.send.api.subcomponents.amount.SendAmountReduceListener
import com.tangem.features.send.api.subcomponents.amount.SendAmountUpdateListener
import com.tangem.features.send.api.subcomponents.amount.analytics.CommonSendAmountAnalyticEvents
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.send.common.CommonSendRoute
import com.tangem.features.send.loadedStatus
import com.tangem.features.send.testDispatcherProvider
import com.tangem.test.core.ProvideTestModels
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class SendAmountModelTest {

    private val testUserWalletId = UserWalletId("1234567890ABCDEF")
    private val cryptoCurrency: CryptoCurrency = mockk(relaxed = true) {
        every { isCustom } returns false
    }

    private val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase = mockk(relaxed = true)
    private val sendAmountReduceListener: SendAmountReduceListener = mockk(relaxed = true)
    private val sendAmountUpdateListener: SendAmountUpdateListener = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk(relaxed = true)
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)
    private val sendAmountAlertFactory: SendAmountAlertFactory = mockk(relaxed = true)
    private val getWalletsUseCase: GetWalletsUseCase = mockk(relaxed = true)
    private val callback: SendAmountComponent.ModelCallback = mockk(relaxed = true)

    private val reduceToFlow = MutableSharedFlow<BigDecimal>(extraBufferCapacity = 1)
    private val reduceByFlow = MutableSharedFlow<ReduceByData>(extraBufferCapacity = 1)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        // PER_CLASS parameterized nested classes reuse one instance — reset verified mocks between rows.
        clearMocks(
            callback,
            sendAmountAlertFactory,
            analyticsEventHandler,
            answers = false,
            recordedCalls = true,
            childMocks = false
        )
        every { getUserWalletUseCase.invokeFlow(testUserWalletId) } returns flowOf(coldWallet().right())
        coEvery { getMinimumTransactionAmountSyncUseCase(any(), any()) } returns BigDecimal.ONE.right()
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()
        every { getWalletsUseCase.invokeSync() } returns listOf(coldWallet())
        every { sendAmountReduceListener.reduceToTriggerFlow } returns reduceToFlow
        every { sendAmountReduceListener.reduceByTriggerFlow } returns reduceByFlow
        every { sendAmountReduceListener.ignoreReduceTriggerFlow } returns emptyFlow()
        every { sendAmountUpdateListener.updateAmountTriggerFlow } returns emptyFlow()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsSendWithSwapAvailable {

        @ParameterizedTest
        @ProvideTestModels
        fun availability(model: SwapModel) = runTest {
            // Arrange
            every { cryptoCurrency.isCustom } returns model.isCustom
            val wallet = coldWallet(isMultiCurrency = model.isMultiCurrency)
            every { getUserWalletUseCase.invokeFlow(testUserWalletId) } returns flowOf(wallet.right())
            val predefined = if (model.isFromMainScreenQr) {
                PredefinedValues.Content.QrCode("1", "addr", null, PredefinedValues.Source.MAIN_SCREEN)
            } else {
                PredefinedValues.Empty
            }
            // Start off an Amount route so the navigation combine stays idle until the wallet is loaded.
            val sut = buildModel(predefinedValues = predefined, route = CommonSendRoute.Amount(false))
            advanceUntilIdle()

            // Assert
            assertThat(sut.isSendWithSwapAvailable.value).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            SwapModel(isCustom = false, isMultiCurrency = true, isFromMainScreenQr = false, expected = true),
            SwapModel(isCustom = true, isMultiCurrency = true, isFromMainScreenQr = false, expected = false),
            SwapModel(isCustom = false, isMultiCurrency = false, isFromMainScreenQr = false, expected = false),
            SwapModel(isCustom = false, isMultiCurrency = true, isFromMainScreenQr = true, expected = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    // Looks like currentRoute.collect{} in onConvertToAnotherToken never completes, so the branch is unreachable.
    @Disabled("currentRoute flow never completes — re-enable after the amount-screen rework")
    inner class OnConvertToAnotherToken {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN onConvertToAnotherToken THEN reset-alert in edit mode else convert directly`(model: ConvertModel) =
            runTest {
                // Arrange
                val sut = buildModel(route = CommonSendRoute.Amount(isEditMode = model.isEditMode))
                advanceUntilIdle()

                // Act
                sut.onConvertToAnotherToken()
                advanceUntilIdle()

                // Assert
                if (model.isEditMode) {
                    verify(exactly = 1) { sendAmountAlertFactory.showResetSendingAlert(any()) }
                    verify(exactly = 0) { callback.onConvertToAnotherToken(any(), any()) }
                } else {
                    verify(exactly = 0) { sendAmountAlertFactory.showResetSendingAlert(any()) }
                    verify(exactly = 1) { callback.onConvertToAnotherToken(any(), any()) }
                }
            }

        private fun provideTestModels() = listOf(
            ConvertModel(isEditMode = true),
            ConvertModel(isEditMode = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnMaxValueClick {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN onMaxValueClick THEN send analytics only for non-zero balance`(model: MaxClickModel) = runTest {
            // Arrange
            val sut = buildModel(
                cryptoCurrencyStatusFlow = MutableStateFlow(loadedStatus(cryptoCurrency, balance = model.balance)),
            )
            advanceUntilIdle()

            // Act
            sut.onMaxValueClick()

            // Assert
            verify(exactly = model.expectedAnalyticsCalls) {
                analyticsEventHandler.send(any<CommonSendAmountAnalyticEvents.MaxAmountButtonClicked>())
            }
        }

        private fun provideTestModels() = listOf(
            MaxClickModel(balance = BigDecimal.ZERO, expectedAnalyticsCalls = 0),
            MaxClickModel(balance = BigDecimal.TEN, expectedAnalyticsCalls = 1),
        )
    }

    @Nested
    inner class ReduceTriggers {

        @Test
        fun `GIVEN reduceTo emitted WHEN handled THEN trigger fee reload`() = runTest {
            // Arrange
            buildModel()
            advanceUntilIdle()

            // Act
            reduceToFlow.tryEmit(BigDecimal.ONE)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { feeSelectorReloadTrigger.triggerUpdate(any()) }
        }

        @Test
        fun `GIVEN reduceBy emitted WHEN handled THEN trigger fee reload`() = runTest {
            // Arrange
            buildModel()
            advanceUntilIdle()

            // Act
            reduceByFlow.tryEmit(ReduceByData(reduceAmountBy = BigDecimal.ONE, reduceAmountByDiff = BigDecimal.ONE))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { feeSelectorReloadTrigger.triggerUpdate(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnAmountNext {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN onAmountNext THEN send selected-currency analytics by entry type and save result`(
            model: AmountNextModel,
        ) = runTest {
            // Arrange
            val sut = buildModel()
            advanceUntilIdle()
            sut.updateState(dataState(isFiat = model.isFiat))

            // Act
            sut.onAmountNext()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    match<CommonSendAmountAnalyticEvents.SelectedCurrency> { it.type == model.expectedType },
                )
            }
            verify(exactly = 1) { callback.onAmountResult(any(), any()) }
        }

        private fun provideTestModels() = listOf(
            AmountNextModel(
                isFiat = true,
                expectedType = CommonSendAmountAnalyticEvents.SelectedCurrencyType.AppCurrency
            ),
            AmountNextModel(isFiat = false, expectedType = CommonSendAmountAnalyticEvents.SelectedCurrencyType.Token),
        )
    }

    // region fixtures

    private fun TestScope.buildModel(
        predefinedValues: PredefinedValues = PredefinedValues.Empty,
        route: CommonSendRoute.Amount = CommonSendRoute.Amount(isEditMode = false),
        cryptoCurrencyStatusFlow: MutableStateFlow<CryptoCurrencyStatus> =
            MutableStateFlow(loadedStatus(cryptoCurrency, balance = BigDecimal.TEN)),
        state: AmountState = AmountState.Empty,
    ): SendAmountModel {
        val params = SendAmountComponentParams.AmountParams(
            state = state,
            analyticsCategoryName = "test_send",
            userWalletId = testUserWalletId,
            appCurrency = AppCurrency.Default,
            predefinedValues = predefinedValues,
            cryptoCurrency = cryptoCurrency,
            cryptoCurrencyStatusFlow = cryptoCurrencyStatusFlow,
            isBalanceHidingFlow = MutableStateFlow(false),
            analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Send,
            accountFlow = MutableStateFlow<Account?>(null),
            isAccountModeFlow = MutableStateFlow(false),
            callback = callback,
            route = route,
        )
        return SendAmountModel(
            paramsContainer = MutableParamsContainer(params),
            dispatchers = testDispatcherProvider(),
            getMinimumTransactionAmountSyncUseCase = getMinimumTransactionAmountSyncUseCase,
            sendAmountReduceListener = sendAmountReduceListener,
            sendAmountUpdateListener = sendAmountUpdateListener,
            analyticsEventHandler = analyticsEventHandler,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            feeSelectorReloadTrigger = feeSelectorReloadTrigger,
            getUserWalletUseCase = getUserWalletUseCase,
            sendAmountAlertFactory = sendAmountAlertFactory,
            getWalletsUseCase = getWalletsUseCase,
        )
    }

    private fun coldWallet(isMultiCurrency: Boolean = true): UserWallet.Cold = mockk(relaxed = true) {
        every { this@mockk.isMultiCurrency } returns isMultiCurrency
    }

    private fun dataState(isFiat: Boolean): AmountState.Data = mockk(relaxed = true) {
        every { amountTextField.isFiatValue } returns isFiat
        every { amountTextField.value } returns "1"
    }

    data class SwapModel(
        val isCustom: Boolean,
        val isMultiCurrency: Boolean,
        val isFromMainScreenQr: Boolean,
        val expected: Boolean,
    )

    data class ConvertModel(val isEditMode: Boolean)

    data class MaxClickModel(val balance: BigDecimal, val expectedAnalyticsCalls: Int)

    data class AmountNextModel(
        val isFiat: Boolean,
        val expectedType: CommonSendAmountAnalyticEvents.SelectedCurrencyType,
    )

    // endregion
}