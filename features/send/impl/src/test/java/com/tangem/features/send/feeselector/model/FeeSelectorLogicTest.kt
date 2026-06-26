package com.tangem.features.send.feeselector.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.domain.transaction.usecase.gasless.GetAvailableFeeTokensUseCase
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeExtraInfo
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams.FeeStateConfiguration
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorCheckReloadListener
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorCheckReloadTrigger
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorReloadListener
import com.tangem.features.send.api.subcomponents.feeSelector.analytics.CommonSendFeeAnalyticEvents
import com.tangem.features.send.loadedStatus
import com.tangem.test.core.ProvideTestModels
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class FeeSelectorLogicTest {

    private val testUserWalletId = UserWalletId("1234567890ABCDEF")
    private val coinStatus: CryptoCurrencyStatus = loadedStatus(mockk<CryptoCurrency.Coin>(relaxed = true))
    private val tokenStatus: CryptoCurrencyStatus = loadedStatus(mockk<CryptoCurrency.Token>(relaxed = true))

    private val isFeeApproximateUseCase: IsFeeApproximateUseCase = mockk(relaxed = true)
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk(relaxed = true)
    private val feeSelectorReloadListener: FeeSelectorReloadListener = mockk(relaxed = true)
    private val feeSelectorCheckReloadListener: FeeSelectorCheckReloadListener = mockk(relaxed = true)
    private val feeSelectorCheckReloadTrigger: FeeSelectorCheckReloadTrigger = mockk(relaxed = true)
    private val feeSelectorAlertFactory: FeeSelectorAlertFactory = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)
    private val getAvailableFeeTokensUseCase: GetAvailableFeeTokensUseCase = mockk(relaxed = true)
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork = mockk(relaxed = true)

    private val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee> = mockk()
    private val onLoadFeeExtended: suspend (CryptoCurrencyStatus?) -> Either<GetFeeError, TransactionFeeExtended> =
        mockk()

    private val checkReloadTriggerFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        // PER_CLASS parameterized nested classes reuse one instance — reset analytics recorded calls between rows.
        clearMocks(analyticsEventHandler, answers = false, recordedCalls = true, childMocks = false)
        coEvery { onLoadFee() } returns GetFeeError.UnknownError.left()
        coEvery { onLoadFeeExtended(any()) } returns GetFeeError.UnknownError.left()
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()
        every { feeSelectorReloadListener.reloadTriggerFlow } returns emptyFlow()
        every { feeSelectorReloadListener.loadingStateTriggerFlow } returns emptyFlow()
        every { feeSelectorCheckReloadListener.checkReloadTriggerFlow } returns checkReloadTriggerFlow
        every { isGaslessFeeSupportedForNetwork(any()) } returns false
        every { isFeeApproximateUseCase(any(), any()) } returns false
    }

    @Nested
    inner class CallLoadFee {

        @Test
        fun `GIVEN gasless disabled WHEN load fee THEN use basic onLoadFee only`() =
            runTest(UnconfinedTestDispatcher()) {
                // Act — init triggers loadFee()
                buildModel(gaslessEnabled = false)
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) { onLoadFee() }
                coVerify(exactly = 0) { onLoadFeeExtended(any()) }
            }

        @Test
        fun `GIVEN gasless not enough funds WHEN load fee THEN surface error without basic fallback`() =
            runTest(UnconfinedTestDispatcher()) {
                // Arrange
                coEvery { onLoadFeeExtended(any()) } returns GetFeeError.GaslessError.NotEnoughFunds.left()

                // Act
                val sut = buildModel(gaslessEnabled = true)
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) { onLoadFeeExtended(any()) }
                coVerify(exactly = 0) { onLoadFee() }
                assertThat(sut.uiState.value).isInstanceOf(FeeSelectorUM.Error::class.java)
            }

        @Test
        fun `GIVEN gasless generic error WHEN load fee THEN fallback to basic and show only speed option`() =
            runTest(UnconfinedTestDispatcher()) {
                // Arrange
                coEvery { onLoadFeeExtended(any()) } returns GetFeeError.GaslessError.NetworkIsNotSupported.left()
                coEvery { onLoadFee() } returns GetFeeError.UnknownError.left()

                // Act
                val sut = buildModel(gaslessEnabled = true)
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) { onLoadFeeExtended(any()) }
                coVerify(exactly = 1) { onLoadFee() }
                assertThat(sut.shouldShowOnlySpeedOption.value).isTrue()
            }

        @Test
        fun `GIVEN gasless success WHEN load fee THEN use extended and clear speed-only option`() =
            runTest(UnconfinedTestDispatcher()) {
                // Arrange — populateExtendedFee then fails (token not found) but the dispatch decision is already made
                val feeExtended = TransactionFeeExtended(
                    transactionFee = singleFee(),
                    feeTokenId = mockk(relaxed = true), // != feeCryptoCurrencyStatus.currency.id -> token lookup
                )
                coEvery { onLoadFeeExtended(any()) } returns feeExtended.right()
                coEvery { singleAccountStatusListSupplier.getSyncOrNull(any<UserWalletId>()) } returns null

                // Act
                val sut = buildModel(gaslessEnabled = true)
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) { onLoadFeeExtended(any()) }
                assertThat(sut.shouldShowOnlySpeedOption.value).isFalse()
            }
    }

    @Nested
    inner class CheckLoadFee {

        @Test
        fun `GIVEN fee reloads successfully WHEN check requested THEN show fee-updated alert`() =
            runTest(UnconfinedTestDispatcher()) {
                // Arrange
                coEvery { onLoadFee() } returns singleFee().right()
                buildModel(gaslessEnabled = false)
                advanceUntilIdle()

                // Act
                checkReloadTriggerFlow.tryEmit(Unit)
                advanceUntilIdle()

                // Assert
                verify(atLeast = 1) { feeSelectorAlertFactory.getFeeUpdatedAlert(any(), any(), any(), any()) }
            }

        @Test
        fun `GIVEN fee reload fails WHEN check requested THEN report failure and show unreachable error`() =
            runTest(UnconfinedTestDispatcher()) {
                // Arrange
                coEvery { onLoadFee() } returns GetFeeError.UnknownError.left()
                buildModel(gaslessEnabled = false)
                advanceUntilIdle()

                // Act
                checkReloadTriggerFlow.tryEmit(Unit)
                advanceUntilIdle()

                // Assert
                coVerify(atLeast = 1) { feeSelectorCheckReloadTrigger.callbackCheckResult(false) }
                verify(atLeast = 1) { feeSelectorAlertFactory.getFeeUnreachableErrorState(any()) }
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnFeeItemSelected {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN fee item selected THEN send custom-fee analytics only for custom`(model: FeeItemSelectedModel) =
            runTest(UnconfinedTestDispatcher()) {
                // Arrange
                val sut = buildModel(gaslessEnabled = false)
                advanceUntilIdle()

                // Act
                sut.onFeeItemSelected(model.feeItem)

                // Assert
                verify(exactly = model.expectedAnalyticsCalls) {
                    analyticsEventHandler.send(ofType<CommonSendFeeAnalyticEvents.CustomFeeButtonClicked>())
                }
            }

        private fun provideTestModels() = listOf(
            FeeItemSelectedModel(
                feeItem = FeeItem.Custom(fee = realFee(), customValues = persistentListOf()),
                expectedAnalyticsCalls = 1,
            ),
            FeeItemSelectedModel(feeItem = FeeItem.Market(fee = realFee()), expectedAnalyticsCalls = 0),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnDoneClick {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN done THEN always send selected-fee and gas-price only for edited custom`(model: DoneClickModel) =
            runTest(UnconfinedTestDispatcher()) {
                // Arrange
                val sut = buildModel(gaslessEnabled = false)
                advanceUntilIdle()
                sut.uiState.value = contentState(selected = model.selected, normalValue = model.normalValue)

                // Act
                sut.onDoneClick()

                // Assert
                verify(exactly = 1) { analyticsEventHandler.send(ofType<CommonSendFeeAnalyticEvents.SelectedFee>()) }
                verify(exactly = model.expectedGasPriceCalls) { analyticsEventHandler.send(ofType<CommonSendFeeAnalyticEvents.GasPriceInserter>()) }
            }

        private fun provideTestModels() = listOf(
            // not custom -> no gas-price
            DoneClickModel(
                selected = FeeItem.Market(realFee("0.001")),
                normalValue = "0.001",
                expectedGasPriceCalls = 0
            ),
            // custom but unedited (== normal) -> no gas-price
            DoneClickModel(
                selected = FeeItem.Custom(realFee("0.001"), persistentListOf()),
                normalValue = "0.001",
                expectedGasPriceCalls = 0,
            ),
            // custom edited (!= normal) -> gas-price
            DoneClickModel(
                selected = FeeItem.Custom(realFee("0.005"), persistentListOf()),
                normalValue = "0.001",
                expectedGasPriceCalls = 1,
            ),
        )
    }

    // region fixtures

    private fun TestScope.buildModel(gaslessEnabled: Boolean): FeeSelectorLogic {
        val currencyStatus = if (gaslessEnabled) tokenStatus else coinStatus
        every { isGaslessFeeSupportedForNetwork(any()) } returns gaslessEnabled
        val params = FeeSelectorParams.FeeSelectorBlockParams(
            state = FeeSelectorUM.Loading,
            userWalletId = testUserWalletId,
            onLoadFeeExtended = if (gaslessEnabled) onLoadFeeExtended else null,
            onLoadFee = onLoadFee,
            cryptoCurrencyStatus = currencyStatus,
            feeCryptoCurrencyStatus = currencyStatus,
            feeStateConfiguration = FeeStateConfiguration.None,
            feeDisplaySource = FeeSelectorParams.FeeDisplaySource.BottomSheet,
            analyticsCategoryName = "test_fee",
            analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Send,
        )
        return FeeSelectorLogic(
            params = params,
            modelScope = backgroundScope,
            isFeeApproximateUseCase = isFeeApproximateUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            feeSelectorReloadListener = feeSelectorReloadListener,
            feeSelectorCheckReloadListener = feeSelectorCheckReloadListener,
            feeSelectorCheckReloadTrigger = feeSelectorCheckReloadTrigger,
            feeSelectorAlertFactory = feeSelectorAlertFactory,
            analyticsEventHandler = analyticsEventHandler,
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            getUserWalletUseCase = getUserWalletUseCase,
            getAvailableFeeTokensUseCase = getAvailableFeeTokensUseCase,
            isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
        )
    }

    private fun contentState(selected: FeeItem, normalValue: String): FeeSelectorUM.Content {
        val extraInfo = FeeExtraInfo(
            isFeeApproximate = false,
            isFeeConvertibleToFiat = true,
            isTronToken = false,
            feeCryptoCurrencyStatus = coinStatus,
        )
        return FeeSelectorUM.Content(
            isPrimaryButtonEnabled = true,
            fees = singleFee(normalValue),
            feeItems = persistentListOf(selected),
            selectedFeeItem = selected,
            feeExtraInfo = extraInfo,
            feeFiatRateUM = null,
            feeNonce = FeeNonce.None,
        )
    }

    private fun realFee(value: String = "0.001"): Fee = Fee.Common(
        Amount(currencySymbol = "ETH", value = BigDecimal(value), decimals = 18),
    )

    private fun singleFee(value: String = "0.001"): TransactionFee = TransactionFee.Single(normal = realFee(value))

    data class FeeItemSelectedModel(val feeItem: FeeItem, val expectedAnalyticsCalls: Int)

    data class DoneClickModel(val selected: FeeItem, val normalValue: String, val expectedGasPriceCalls: Int)

    // endregion
}