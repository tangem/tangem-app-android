package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRouter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.FeeItemState
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class StateBuilderQuotesTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val isBalanceHiddenProvider: Provider<Boolean> = mockk()
    private val appCurrencyProvider: Provider<AppCurrency> = mockk()
    private val isAccountsModeProvider: Provider<Boolean> = mockk()
    private val iGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork = mockk()
    private val appRouter: AppRouter = mockk()

    private lateinit var sut: StateBuilder

    private val userWalletId = UserWalletId("aabbccdd")
    private val coldWallet: UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }
    private val hotWallet: UserWallet.Hot = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

    private val emptyAmountState = SwapState.EmptyAmountState(
        zeroAmountEquivalent = com.tangem.core.ui.extensions.stringReference("$0.00"),
    )

    @BeforeEach
    fun setup() {
        every { isBalanceHiddenProvider() } returns false
        every { appCurrencyProvider() } returns AppCurrency.Default
        every { isAccountsModeProvider() } returns false
        every { iGaslessFeeSupportedForNetwork(any()) } returns false

        sut = StateBuilder(
            actions = actions,
            isBalanceHiddenProvider = isBalanceHiddenProvider,
            appCurrencyProvider = appCurrencyProvider,
            isAccountsModeProvider = isAccountsModeProvider,
            iGaslessFeeSupportedForNetwork = iGaslessFeeSupportedForNetwork,
            appRouter = appRouter,
        )
    }

    // region createQuotesLoadingState

    @Nested
    inner class CreateQuotesLoadingState {

        @Test
        fun `GIVEN uiState has Empty sendCard WHEN called THEN returns uiState unchanged`() {
            val loadingState = sut.createInitialLoadingState()
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createQuotesLoadingState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                uiStateHolder = loadingState,
            )

            assertThat(result).isSameInstanceAs(loadingState)
        }

        @Test
        fun `GIVEN valid SwapCardData state WHEN called THEN providerState is Loading`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createQuotesLoadingState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                uiStateHolder = baseState,
            )

            assertThat(result.providerState).isInstanceOf(ProviderState.Loading::class.java)
        }

        @Test
        fun `GIVEN valid state WHEN called THEN changeCardsButtonState is UPDATE_IN_PROGRESS`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createQuotesLoadingState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                uiStateHolder = baseState,
            )

            assertThat(result.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.UPDATE_IN_PROGRESS)
        }

        @Test
        fun `GIVEN valid state WHEN called THEN swapButton is disabled`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createQuotesLoadingState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                uiStateHolder = baseState,
            )

            assertThat(result.swapButton.isEnabled).isFalse()
        }

        @Test
        fun `GIVEN valid state WHEN called THEN fee is Empty`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createQuotesLoadingState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                uiStateHolder = baseState,
            )

            assertThat(result.fee).isInstanceOf(FeeItemState.Empty::class.java)
        }

        @Test
        fun `GIVEN valid state WHEN called THEN notifications is cleared`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createQuotesLoadingState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                uiStateHolder = baseState,
            )

            assertThat(result.notifications).isEmpty()
        }

        @Test
        fun `GIVEN hot wallet WHEN called THEN swapButton isHoldToConfirm is true`() {
            val baseState = buildReadyState(hotWallet)
            val fromStatus = buildSwapCurrencyStatus(hotWallet)
            val toStatus = buildSwapCurrencyStatus(hotWallet)

            val result = sut.createQuotesLoadingState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                uiStateHolder = baseState,
            )

            assertThat(result.swapButton.isHoldToConfirm).isTrue()
        }

        @Test
        fun `GIVEN valid state WHEN called THEN receiveCardData amountTextFieldValue is null`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createQuotesLoadingState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                uiStateHolder = baseState,
            )

            val receiveCard = result.receiveCardData as? SwapCardState.SwapCardData
            assertThat(receiveCard?.amountTextFieldValue).isNull()
        }
    }

    // endregion

    // region createQuotesLoadedState

    @Nested
    inner class CreateQuotesLoadedState {

        @Test
        fun `GIVEN uiState has Empty sendCard WHEN called THEN returns uiState unchanged`() {
            val loadingState = sut.createInitialLoadingState()
            val quoteModel = buildQuoteModel(coldWallet, isBalanceEnough = true)
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = loadingState,
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapProvider = swapProvider,
                bestRatedProviderId = "provider-id",
                isNeedBestRateBadge = false,
                selectedFeeType = FeeType.NORMAL,
                needApplyFCARestrictions = false,
                hideFee = false,
            )

            assertThat(result).isSameInstanceAs(loadingState)
        }

        @Test
        fun `GIVEN valid state with hideFee true WHEN called THEN fee is Empty`() {
            val baseState = buildReadyState(coldWallet)
            val quoteModel = buildQuoteModel(coldWallet, isBalanceEnough = true)
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseState,
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapProvider = swapProvider,
                bestRatedProviderId = "provider-id",
                isNeedBestRateBadge = false,
                selectedFeeType = FeeType.NORMAL,
                needApplyFCARestrictions = false,
                hideFee = true,
            )

            assertThat(result.fee).isInstanceOf(FeeItemState.Empty::class.java)
        }

        @Test
        fun `GIVEN valid state with hideFee false and single fee WHEN called THEN fee is Content`() {
            val baseState = buildReadyState(coldWallet)
            val quoteModel = buildQuoteModel(
                userWallet = coldWallet,
                isBalanceEnough = true,
                txFeeState = TxFeeState.SingleFeeState(fee = buildTxFeeLegacy(FeeType.NORMAL)),
            )
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseState,
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapProvider = swapProvider,
                bestRatedProviderId = "provider-id",
                isNeedBestRateBadge = false,
                selectedFeeType = FeeType.NORMAL,
                needApplyFCARestrictions = false,
                hideFee = false,
            )

            assertThat(result.fee).isInstanceOf(FeeItemState.Content::class.java)
        }

        @Test
        fun `GIVEN valid state with sufficient balance WHEN called THEN isInsufficientFunds is false`() {
            val baseState = buildReadyState(coldWallet)
            val quoteModel = buildQuoteModel(coldWallet, isBalanceEnough = true)
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseState,
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapProvider = swapProvider,
                bestRatedProviderId = "provider-id",
                isNeedBestRateBadge = false,
                selectedFeeType = FeeType.NORMAL,
                needApplyFCARestrictions = false,
                hideFee = false,
            )

            assertThat(result.isInsufficientFunds).isFalse()
        }

        @Test
        fun `GIVEN valid state with insufficient balance WHEN called THEN isInsufficientFunds is true`() {
            val baseState = buildReadyState(coldWallet)
            val quoteModel = buildQuoteModel(
                coldWallet,
                isBalanceEnough = false,
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
            )
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseState,
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapProvider = swapProvider,
                bestRatedProviderId = "provider-id",
                isNeedBestRateBadge = false,
                selectedFeeType = FeeType.NORMAL,
                needApplyFCARestrictions = false,
                hideFee = false,
            )

            assertThat(result.isInsufficientFunds).isTrue()
        }

        @Test
        fun `GIVEN valid state WHEN called THEN changeCardsButtonState is ENABLED`() {
            val baseState = buildReadyState(coldWallet)
            val quoteModel = buildQuoteModel(coldWallet, isBalanceEnough = true)
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseState,
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapProvider = swapProvider,
                bestRatedProviderId = "provider-id",
                isNeedBestRateBadge = false,
                selectedFeeType = FeeType.NORMAL,
                needApplyFCARestrictions = false,
                hideFee = false,
            )

            assertThat(result.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.ENABLED)
        }

        @Test
        fun `GIVEN valid state with hot wallet WHEN called THEN swapButton isHoldToConfirm is true`() {
            val baseState = buildReadyState(hotWallet)
            val quoteModel = buildQuoteModel(hotWallet, isBalanceEnough = true)
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseState,
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapProvider = swapProvider,
                bestRatedProviderId = "provider-id",
                isNeedBestRateBadge = false,
                selectedFeeType = FeeType.NORMAL,
                needApplyFCARestrictions = false,
                hideFee = false,
            )

            assertThat(result.swapButton.isHoldToConfirm).isTrue()
        }

        @Test
        fun `GIVEN provider with termsOfUse WHEN called THEN tosState has tosLink`() {
            val baseState = buildReadyState(coldWallet)
            val quoteModel = buildQuoteModel(coldWallet, isBalanceEnough = true)
            val swapProvider = buildSwapProvider(termsOfUse = "https://example.com/tos")

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseState,
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapProvider = swapProvider,
                bestRatedProviderId = "provider-id",
                isNeedBestRateBadge = false,
                selectedFeeType = FeeType.NORMAL,
                needApplyFCARestrictions = false,
                hideFee = false,
            )

            assertThat(result.tosState?.tosLink).isNotNull()
        }

        @Test
        fun `GIVEN provider without termsOfUse WHEN called THEN tosState has null tosLink`() {
            val baseState = buildReadyState(coldWallet)
            val quoteModel = buildQuoteModel(coldWallet, isBalanceEnough = true)
            val swapProvider = buildSwapProvider(termsOfUse = null)

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseState,
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapProvider = swapProvider,
                bestRatedProviderId = "provider-id",
                isNeedBestRateBadge = false,
                selectedFeeType = FeeType.NORMAL,
                needApplyFCARestrictions = false,
                hideFee = false,
            )

            assertThat(result.tosState?.tosLink).isNull()
        }

        @Test
        fun `GIVEN no blocking notifications WHEN called THEN swapButton is enabled`() {
            val baseState = buildReadyState(coldWallet)
            val quoteModel = buildQuoteModel(coldWallet, isBalanceEnough = true)
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseState,
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapProvider = swapProvider,
                bestRatedProviderId = "provider-id",
                isNeedBestRateBadge = false,
                selectedFeeType = FeeType.NORMAL,
                needApplyFCARestrictions = false,
                hideFee = false,
            )

            assertThat(result.swapButton.isEnabled).isTrue()
        }

        @Test
        fun `GIVEN multiple fee state WHEN called THEN fee is Content with isClickable true`() {
            val baseState = buildReadyState(coldWallet)
            val quoteModel = buildQuoteModel(
                userWallet = coldWallet,
                isBalanceEnough = true,
                txFeeState = TxFeeState.MultipleFeeState(
                    normalFee = buildTxFeeLegacy(FeeType.NORMAL),
                    priorityFee = buildTxFeeLegacy(FeeType.PRIORITY),
                ),
            )
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseState,
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapProvider = swapProvider,
                bestRatedProviderId = "provider-id",
                isNeedBestRateBadge = false,
                selectedFeeType = FeeType.NORMAL,
                needApplyFCARestrictions = false,
                hideFee = false,
            )

            val feeContent = result.fee as? FeeItemState.Content
            assertThat(feeContent?.isClickable).isTrue()
        }
    }

    // endregion

    // region createQuotesErrorState

    @Nested
    inner class CreateQuotesErrorState {

        @Test
        fun `GIVEN uiState has Empty sendCard WHEN called THEN returns uiState unchanged`() {
            val loadingState = sut.createInitialLoadingState()
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val fromTokenInfo = TokenSwapInfo(
                tokenAmount = buildSwapAmount(),
                amountFiat = BigDecimal.ZERO,
                swapCurrencyStatus = fromStatus,
            )
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesErrorState(
                uiStateHolder = loadingState,
                swapProvider = swapProvider,
                fromToken = fromTokenInfo,
                toSwapCurrencyStatus = null,
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                expressDataError = ExpressDataError.UnknownError,
                needApplyFCARestrictions = false,
            )

            assertThat(result).isSameInstanceAs(loadingState)
        }

        @Test
        fun `GIVEN valid state WHEN called THEN swapButton is disabled`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val fromTokenInfo = buildTokenSwapInfo(fromStatus)
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesErrorState(
                uiStateHolder = baseState,
                swapProvider = swapProvider,
                fromToken = fromTokenInfo,
                toSwapCurrencyStatus = null,
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                expressDataError = ExpressDataError.UnknownError,
                needApplyFCARestrictions = false,
            )

            assertThat(result.swapButton.isEnabled).isFalse()
        }

        @Test
        fun `GIVEN valid state WHEN called THEN fee is Empty`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val fromTokenInfo = buildTokenSwapInfo(fromStatus)
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesErrorState(
                uiStateHolder = baseState,
                swapProvider = swapProvider,
                fromToken = fromTokenInfo,
                toSwapCurrencyStatus = null,
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                expressDataError = ExpressDataError.UnknownError,
                needApplyFCARestrictions = false,
            )

            assertThat(result.fee).isInstanceOf(FeeItemState.Empty::class.java)
        }

        @Test
        fun `GIVEN valid state WHEN called THEN permissionUM is Empty`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val fromTokenInfo = buildTokenSwapInfo(fromStatus)
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesErrorState(
                uiStateHolder = baseState,
                swapProvider = swapProvider,
                fromToken = fromTokenInfo,
                toSwapCurrencyStatus = null,
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                expressDataError = ExpressDataError.UnknownError,
                needApplyFCARestrictions = false,
            )

            assertThat(result.permissionUM).isEqualTo(SwapPermissionUM.Empty)
        }

        @Test
        fun `GIVEN toSwapCurrencyStatus null WHEN called THEN receiveCardData is Empty`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val fromTokenInfo = buildTokenSwapInfo(fromStatus)
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesErrorState(
                uiStateHolder = baseState,
                swapProvider = swapProvider,
                fromToken = fromTokenInfo,
                toSwapCurrencyStatus = null,
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                expressDataError = ExpressDataError.UnknownError,
                needApplyFCARestrictions = false,
            )

            assertThat(result.receiveCardData).isInstanceOf(SwapCardState.Empty::class.java)
        }

        @Test
        fun `GIVEN toSwapCurrencyStatus non-null WHEN called THEN receiveCardData is SwapCardData`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)
            val fromTokenInfo = buildTokenSwapInfo(fromStatus)
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesErrorState(
                uiStateHolder = baseState,
                swapProvider = swapProvider,
                fromToken = fromTokenInfo,
                toSwapCurrencyStatus = toStatus,
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                expressDataError = ExpressDataError.UnknownError,
                needApplyFCARestrictions = false,
            )

            assertThat(result.receiveCardData).isInstanceOf(SwapCardState.SwapCardData::class.java)
        }

        @Test
        fun `GIVEN ExchangeTooSmallAmountError WHEN called THEN providerState is Content`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val fromTokenInfo = buildTokenSwapInfo(fromStatus)
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesErrorState(
                uiStateHolder = baseState,
                swapProvider = swapProvider,
                fromToken = fromTokenInfo,
                toSwapCurrencyStatus = null,
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                expressDataError = ExpressDataError.ExchangeTooSmallAmountError(
                    amount = buildSwapAmount(),
                    code = 100,
                ),
                needApplyFCARestrictions = false,
            )

            assertThat(result.providerState).isInstanceOf(ProviderState.Content::class.java)
        }

        @Test
        fun `GIVEN UnknownError WHEN called THEN providerState is Empty`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val fromTokenInfo = buildTokenSwapInfo(fromStatus)
            val swapProvider = buildSwapProvider()

            val result = sut.createQuotesErrorState(
                uiStateHolder = baseState,
                swapProvider = swapProvider,
                fromToken = fromTokenInfo,
                toSwapCurrencyStatus = null,
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                expressDataError = ExpressDataError.UnknownError,
                needApplyFCARestrictions = false,
            )

            assertThat(result.providerState).isInstanceOf(ProviderState.Empty::class.java)
        }
    }

    // endregion

    // region createQuotesEmptyAmountState

    @Nested
    inner class CreateQuotesEmptyAmountState {

        @Test
        fun `GIVEN uiState has Empty sendCard WHEN called THEN returns uiState unchanged`() {
            val loadingState = sut.createInitialLoadingState()

            val result = sut.createQuotesEmptyAmountState(
                uiStateHolder = loadingState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
            )

            assertThat(result).isSameInstanceAs(loadingState)
        }

        @Test
        fun `GIVEN valid SwapCardData state WHEN called THEN swapButton is disabled`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.createQuotesEmptyAmountState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
            )

            assertThat(result.swapButton.isEnabled).isFalse()
        }

        @Test
        fun `GIVEN valid state WHEN called THEN notifications is empty`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.createQuotesEmptyAmountState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
            )

            assertThat(result.notifications).isEmpty()
        }

        @Test
        fun `GIVEN valid state WHEN called THEN isInsufficientFunds is false`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.createQuotesEmptyAmountState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
            )

            assertThat(result.isInsufficientFunds).isFalse()
        }

        @Test
        fun `GIVEN valid state WHEN called THEN fee is Empty`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.createQuotesEmptyAmountState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
            )

            assertThat(result.fee).isInstanceOf(FeeItemState.Empty::class.java)
        }

        @Test
        fun `GIVEN valid state WHEN called THEN changeCardsButtonState is ENABLED`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.createQuotesEmptyAmountState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
            )

            assertThat(result.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.ENABLED)
        }

        @Test
        fun `GIVEN valid state WHEN called THEN providerState is Empty`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.createQuotesEmptyAmountState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
            )

            assertThat(result.providerState).isInstanceOf(ProviderState.Empty::class.java)
        }

        @Test
        fun `GIVEN valid state WHEN called THEN receiveCard amountTextFieldValue is 0`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.createQuotesEmptyAmountState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
            )

            val receiveCard = result.receiveCardData as? SwapCardState.SwapCardData
            assertThat(receiveCard?.amountTextFieldValue?.text).isEqualTo("0")
        }

        @Test
        fun `GIVEN fromSwapCurrencyStatus with hot wallet WHEN called THEN swapButton isHoldToConfirm is true`() {
            val baseState = buildReadyState(hotWallet)
            val fromStatus = buildSwapCurrencyStatus(hotWallet)

            val result = sut.createQuotesEmptyAmountState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
            )

            assertThat(result.swapButton.isHoldToConfirm).isTrue()
        }
    }

    // endregion

    // --- Helpers ---

    private fun buildReadyState(userWallet: UserWallet): SwapStateHolder {
        val fromStatus = buildSwapCurrencyStatus(userWallet)
        val toStatus = buildSwapCurrencyStatus(userWallet)
        return sut.createInitialReadyState(
            uiStateHolder = sut.createInitialLoadingState(),
            emptyAmountState = emptyAmountState,
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
        )
    }

    private fun buildQuoteModel(
        userWallet: UserWallet,
        isBalanceEnough: Boolean,
        includeFeeInAmount: IncludeFeeInAmount = IncludeFeeInAmount.Excluded,
        txFeeState: TxFeeState = TxFeeState.Empty,
    ): SwapState.QuotesLoadedState {
        val fromStatus = buildSwapCurrencyStatus(userWallet)
        val toStatus = buildSwapCurrencyStatus(userWallet)

        val fromTokenInfo = TokenSwapInfo(
            tokenAmount = buildSwapAmount(),
            amountFiat = BigDecimal("100.00"),
            swapCurrencyStatus = fromStatus,
        )
        val toTokenInfo = TokenSwapInfo(
            tokenAmount = buildSwapAmount(value = BigDecimal("0.05")),
            amountFiat = BigDecimal("100.00"),
            swapCurrencyStatus = toStatus,
        )

        return SwapState.QuotesLoadedState(
            fromTokenInfo = fromTokenInfo,
            toTokenInfo = toTokenInfo,
            priceImpact = PriceImpact.Empty,
            preparedSwapConfigState = PreparedSwapConfigState(
                isBalanceEnough = isBalanceEnough,
                feeState = SwapFeeState.Enough,
                hasOutgoingTransaction = false,
                includeFeeInAmount = includeFeeInAmount,
            ),
            permissionState = PermissionDataState.Empty,
            txFee = txFeeState,
            currencyCheck = null,
            validationResult = null,
            minAdaValue = null,
            swapProvider = buildSwapProvider(),
        )
    }

    private fun buildSwapProvider(
        termsOfUse: String? = null,
        privacyPolicy: String? = null,
    ) = SwapProvider(
        providerId = "provider-id",
        name = "TestProvider",
        type = ExchangeProviderType.DEX,
        imageLarge = "https://example.com/icon.png",
        termsOfUse = termsOfUse,
        privacyPolicy = privacyPolicy,
        isRecommended = false,
        slippage = null,
    )

    private fun buildSwapAmount(value: BigDecimal = BigDecimal("1.0")) = SwapAmount(
        value = value,
        decimals = 18,
    )

    private fun buildTokenSwapInfo(swapCurrencyStatus: SwapCurrencyStatus) = TokenSwapInfo(
        tokenAmount = buildSwapAmount(),
        amountFiat = BigDecimal.ZERO,
        swapCurrencyStatus = swapCurrencyStatus,
    )

    private fun buildTxFeeLegacy(feeType: FeeType): TxFee.Legacy {
        val fee: com.tangem.blockchain.common.transaction.Fee = mockk(relaxed = true)
        return TxFee.Legacy(
            feeValue = BigDecimal("0.001"),
            feeFiatFormatted = "$2.00",
            feeCryptoFormatted = "0.001 ETH",
            feeIncludeOtherNativeFee = BigDecimal.ZERO,
            feeFiatFormattedWithNative = "$2.00",
            feeCryptoFormattedWithNative = "0.001 ETH",
            cryptoSymbol = "ETH",
            feeType = feeType,
            fee = fee,
        )
    }
}