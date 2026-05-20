package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.FeeItemState
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class StateBuilderInitialStateTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val isBalanceHiddenProvider: Provider<Boolean> = mockk()
    private val appCurrencyProvider: Provider<AppCurrency> = mockk()
    private val isAccountsModeProvider: Provider<Boolean> = mockk()
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork = mockk()
    private val swapFeatureToggles: SwapFeatureToggles = mockk(relaxed = true)

    private lateinit var sut: StateBuilder

    private val appCurrency = AppCurrency.Default

    @BeforeEach
    fun setup() {
        every { isBalanceHiddenProvider() } returns false
        every { appCurrencyProvider() } returns appCurrency
        every { isAccountsModeProvider() } returns false

        sut = StateBuilder(
            actions = actions,
            isBalanceHiddenProvider = isBalanceHiddenProvider,
            appCurrencyProvider = appCurrencyProvider,
            isAccountsModeProvider = isAccountsModeProvider,
            isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
            swapFeatureToggles = swapFeatureToggles,
        )
    }

    // region createInitialLoadingState (no-arg overload)

    @Nested
    inner class `createInitialLoadingState no-arg` {

        @Test
        fun `should return loading state with disabled swap button`() {
            val result = sut.createInitialLoadingState()

            assertThat(result.swapButton.isEnabled).isFalse()
            assertThat(result.swapButton.isInProgress).isTrue()
            assertThat(result.swapButton.isHoldToConfirm).isFalse()
        }

        @Test
        fun `should return loading state with empty send and receive cards`() {
            val result = sut.createInitialLoadingState()

            assertThat(result.sendCardData).isInstanceOf(SwapCardState.Empty::class.java)
            assertThat(result.receiveCardData).isInstanceOf(SwapCardState.Empty::class.java)
        }

        @Test
        fun `should return loading state with Empty fee`() {
            val result = sut.createInitialLoadingState()

            assertThat(result.fee).isInstanceOf(FeeItemState.Empty::class.java)
        }

        @Test
        fun `should return loading state with DISABLED changeCardsButtonState`() {
            val result = sut.createInitialLoadingState()

            assertThat(result.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.DISABLED)
        }

        @Test
        fun `should return loading state with Empty providerState`() {
            val result = sut.createInitialLoadingState()

            assertThat(result.providerState).isInstanceOf(ProviderState.Empty::class.java)
        }

        @Test
        fun `should return loading state with null walletInteractionIcon`() {
            val result = sut.createInitialLoadingState()

            assertThat(result.swapButton.walletInteractionIcon).isNull()
        }
    }

    // endregion

    // region createInitialReadyState

    @Nested
    inner class CreateInitialReadyState {

        private val userWalletId = UserWalletId("aabbccdd")
        private val userWallet: UserWallet.Cold = mockk(relaxed = true) {
            every { walletId } returns userWalletId
        }

        private val emptyAmountState = SwapState.EmptyAmountState(
            zeroAmountEquivalent = com.tangem.core.ui.extensions.stringReference("$0.00"),
        )
        private val baseState get() = sut.createInitialLoadingState()

        @Test
        fun `GIVEN both currencies non-null WHEN called THEN sendCard is SwapCardData`() {
            val fromStatus = buildSwapCurrencyStatus(userWallet)
            val toStatus = buildSwapCurrencyStatus(userWallet)

            val result = sut.createInitialReadyState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.sendCardData).isInstanceOf(SwapCardState.SwapCardData::class.java)
        }

        @Test
        fun `GIVEN both currencies non-null WHEN called THEN receiveCard is SwapCardData`() {
            val fromStatus = buildSwapCurrencyStatus(userWallet)
            val toStatus = buildSwapCurrencyStatus(userWallet)

            val result = sut.createInitialReadyState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.receiveCardData).isInstanceOf(SwapCardState.SwapCardData::class.java)
        }

        @Test
        fun `GIVEN fromCurrency is null WHEN called THEN sendCard is Empty`() {
            val toStatus = buildSwapCurrencyStatus(userWallet)

            val result = sut.createInitialReadyState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.sendCardData).isInstanceOf(SwapCardState.Empty::class.java)
        }

        @Test
        fun `GIVEN fromCurrency is null WHEN called THEN swapButton has no walletInteractionIcon`() {
            val toStatus = buildSwapCurrencyStatus(userWallet)

            val result = sut.createInitialReadyState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.swapButton.walletInteractionIcon).isNull()
        }

        @Test
        fun `GIVEN cold wallet WHEN called THEN swapButton isHoldToConfirm is false`() {
            val fromStatus = buildSwapCurrencyStatus(userWallet)
            val toStatus = buildSwapCurrencyStatus(userWallet)

            val result = sut.createInitialReadyState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.swapButton.isHoldToConfirm).isFalse()
        }

        @Test
        fun `GIVEN hot wallet WHEN called THEN swapButton isHoldToConfirm is true`() {
            val hotWallet: UserWallet.Hot = mockk(relaxed = true) {
                every { walletId } returns userWalletId
            }
            val fromStatus = buildSwapCurrencyStatus(hotWallet)
            val toStatus = buildSwapCurrencyStatus(hotWallet)

            val result = sut.createInitialReadyState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.swapButton.isHoldToConfirm).isTrue()
        }

        @Test
        fun `WHEN called THEN changeCardsButtonState is ENABLED`() {
            val result = sut.createInitialReadyState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
                toSwapCurrencyStatus = null,
            )

            assertThat(result.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.ENABLED)
        }

        @Test
        fun `WHEN called THEN swapButton is disabled`() {
            val fromStatus = buildSwapCurrencyStatus(userWallet)
            val toStatus = buildSwapCurrencyStatus(userWallet)

            val result = sut.createInitialReadyState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.swapButton.isEnabled).isFalse()
        }

        @Test
        fun `WHEN called THEN providerState is Empty`() {
            val result = sut.createInitialReadyState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
                toSwapCurrencyStatus = null,
            )

            assertThat(result.providerState).isInstanceOf(ProviderState.Empty::class.java)
        }

        @Test
        fun `WHEN called THEN priceImpact is Empty`() {
            val result = sut.createInitialReadyState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
                toSwapCurrencyStatus = null,
            )

            assertThat(result.priceImpact).isEqualTo(PriceImpact.Empty)
        }

        @Test
        fun `WHEN called THEN notifications is empty`() {
            val result = sut.createInitialReadyState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
                toSwapCurrencyStatus = null,
            )

            assertThat(result.notifications).isEmpty()
        }
    }

    // endregion

    // region createInitialErrorState

    @Nested
    inner class CreateInitialErrorState {

        private val userWalletId = UserWalletId("aabbccdd")
        private val coldWallet: UserWallet.Cold = mockk(relaxed = true) {
            every { walletId } returns userWalletId
        }
        private val hotWallet: UserWallet.Hot = mockk(relaxed = true) {
            every { walletId } returns userWalletId
        }

        private val expressError: ExpressError = ExpressError.UnknownError

        private fun buildBaseStateWithSwapCardData(userWallet: UserWallet): SwapStateHolder {
            val emptyAmountState = SwapState.EmptyAmountState(
                zeroAmountEquivalent = com.tangem.core.ui.extensions.stringReference("$0.00"),
            )
            val fromStatus = buildSwapCurrencyStatus(userWallet)
            val toStatus = buildSwapCurrencyStatus(userWallet)
            val loading = sut.createInitialLoadingState()
            return sut.createInitialReadyState(
                uiStateHolder = loading,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )
        }

        @Test
        fun `GIVEN fromSwapCurrencyStatus is null WHEN called THEN swapButton comes from uiStateHolder`() {
            val baseState = buildBaseStateWithSwapCardData(coldWallet)
            val originalButton = baseState.swapButton

            val result = sut.createInitialErrorState(
                fromSwapCurrencyStatus = null,
                uiStateHolder = baseState,
                expressError = expressError,
                onRetry = {},
            )

            assertThat(result.swapButton).isEqualTo(originalButton)
        }

        @Test
        fun `GIVEN fromSwapCurrencyStatus non-null with cold wallet WHEN called THEN swapButton isHoldToConfirm is false`() {
            val baseState = buildBaseStateWithSwapCardData(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createInitialErrorState(
                fromSwapCurrencyStatus = fromStatus,
                uiStateHolder = baseState,
                expressError = expressError,
                onRetry = {},
            )

            assertThat(result.swapButton.isHoldToConfirm).isFalse()
        }

        @Test
        fun `GIVEN fromSwapCurrencyStatus non-null with hot wallet WHEN called THEN swapButton isHoldToConfirm is true`() {
            val baseState = buildBaseStateWithSwapCardData(hotWallet)
            val fromStatus = buildSwapCurrencyStatus(hotWallet)

            val result = sut.createInitialErrorState(
                fromSwapCurrencyStatus = fromStatus,
                uiStateHolder = baseState,
                expressError = expressError,
                onRetry = {},
            )

            assertThat(result.swapButton.isHoldToConfirm).isTrue()
        }

        @Test
        fun `WHEN called THEN swapButton is disabled`() {
            val baseState = buildBaseStateWithSwapCardData(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createInitialErrorState(
                fromSwapCurrencyStatus = fromStatus,
                uiStateHolder = baseState,
                expressError = expressError,
                onRetry = {},
            )

            assertThat(result.swapButton.isEnabled).isFalse()
        }

        @Test
        fun `WHEN called THEN notifications contains ExpressErrorWarning`() {
            val baseState = buildBaseStateWithSwapCardData(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createInitialErrorState(
                fromSwapCurrencyStatus = fromStatus,
                uiStateHolder = baseState,
                expressError = expressError,
                onRetry = {},
            )

            assertThat(result.notifications).hasSize(1)
            assertThat(result.notifications[0]).isInstanceOf(SwapNotificationUM.Warning.ExpressErrorWarning::class.java)
        }

        @Test
        fun `WHEN called THEN permissionUM is Empty`() {
            val baseState = buildBaseStateWithSwapCardData(coldWallet)

            val result = sut.createInitialErrorState(
                fromSwapCurrencyStatus = null,
                uiStateHolder = baseState,
                expressError = expressError,
                onRetry = {},
            )

            assertThat(result.permissionUM).isEqualTo(SwapPermissionUM.Empty)
        }

        @Test
        fun `WHEN called THEN fee is Empty`() {
            val baseState = buildBaseStateWithSwapCardData(coldWallet)

            val result = sut.createInitialErrorState(
                fromSwapCurrencyStatus = null,
                uiStateHolder = baseState,
                expressError = expressError,
                onRetry = {},
            )

            assertThat(result.fee).isInstanceOf(FeeItemState.Empty::class.java)
        }

        @Test
        fun `WHEN called THEN changeCardsButtonState is ENABLED`() {
            val baseState = buildBaseStateWithSwapCardData(coldWallet)

            val result = sut.createInitialErrorState(
                fromSwapCurrencyStatus = null,
                uiStateHolder = baseState,
                expressError = expressError,
                onRetry = {},
            )

            assertThat(result.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.ENABLED)
        }

        @Test
        fun `WHEN called THEN tosState is null`() {
            val baseState = buildBaseStateWithSwapCardData(coldWallet)

            val result = sut.createInitialErrorState(
                fromSwapCurrencyStatus = null,
                uiStateHolder = baseState,
                expressError = expressError,
                onRetry = {},
            )

            assertThat(result.tosState).isNull()
        }

        @Test
        fun `GIVEN sendCardData is SwapCardData with Inputtable type WHEN called THEN sendCard type becomes disabled`() {
            val baseState = buildBaseStateWithSwapCardData(coldWallet)

            val result = sut.createInitialErrorState(
                fromSwapCurrencyStatus = null,
                uiStateHolder = baseState,
                expressError = expressError,
                onRetry = {},
            )

            val sendCard = result.sendCardData as? SwapCardState.SwapCardData
            val inputtable = sendCard?.type as? TransactionCardType.Inputtable
            assertThat(inputtable?.isEnabled).isFalse()
        }
    }

    // endregion

    // region createInitialLoadingState (two-arg overload)

    @Nested
    inner class `createInitialLoadingState two-arg overload` {

        private val userWalletId = UserWalletId("aabbccdd")
        private val coldWallet: UserWallet.Cold = mockk(relaxed = true) {
            every { walletId } returns userWalletId
        }

        @Test
        fun `GIVEN uiState has non-SwapCardData send card WHEN called THEN returns uiState unchanged`() {
            val loadingState = sut.createInitialLoadingState()
            // loadingState has SwapCardState.Empty cards — not SwapCardData
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createInitialLoadingState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                uiStateHolder = loadingState,
            )

            assertThat(result).isSameInstanceAs(loadingState)
        }

        @Test
        fun `GIVEN uiState has SwapCardData cards WHEN called THEN changeCardsButtonState is UPDATE_IN_PROGRESS`() {
            val emptyAmountState = SwapState.EmptyAmountState(
                zeroAmountEquivalent = com.tangem.core.ui.extensions.stringReference("$0.00"),
            )
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)
            val readyState = sut.createInitialReadyState(
                uiStateHolder = sut.createInitialLoadingState(),
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            val result = sut.createInitialLoadingState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                uiStateHolder = readyState,
            )

            assertThat(result.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.UPDATE_IN_PROGRESS)
        }

        @Test
        fun `GIVEN cold wallet WHEN called THEN swapButton isHoldToConfirm is false`() {
            val emptyAmountState = SwapState.EmptyAmountState(
                zeroAmountEquivalent = com.tangem.core.ui.extensions.stringReference("$0.00"),
            )
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)
            val readyState = sut.createInitialReadyState(
                uiStateHolder = sut.createInitialLoadingState(),
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            val result = sut.createInitialLoadingState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                uiStateHolder = readyState,
            )

            assertThat(result.swapButton.isHoldToConfirm).isFalse()
        }

        @Test
        fun `GIVEN hot wallet WHEN called THEN swapButton isHoldToConfirm is true`() {
            val hotWallet: UserWallet.Hot = mockk(relaxed = true) {
                every { walletId } returns userWalletId
            }
            val emptyAmountState = SwapState.EmptyAmountState(
                zeroAmountEquivalent = com.tangem.core.ui.extensions.stringReference("$0.00"),
            )
            val fromStatus = buildSwapCurrencyStatus(hotWallet)
            val toStatus = buildSwapCurrencyStatus(hotWallet)
            // need a base state that has SwapCardData — use readyState built with hotWallet
            val readyState = sut.createInitialReadyState(
                uiStateHolder = sut.createInitialLoadingState(),
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            val result = sut.createInitialLoadingState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                uiStateHolder = readyState,
            )

            assertThat(result.swapButton.isHoldToConfirm).isTrue()
        }
    }

    // endregion
}

// --- Helpers shared across StateBuilder test files ---

internal fun buildSwapCurrencyStatus(
    userWallet: UserWallet,
): SwapCurrencyStatus {
    val userWalletId = userWallet.walletId
    val account = Account.CryptoPortfolio.createMainAccount(userWalletId)
    val currency: CryptoCurrency = mockk(relaxed = true) {
        every { symbol } returns "ETH"
        every { decimals } returns 18
        every { name } returns "Ethereum"
        every { network } returns mockk(relaxed = true) {
            every { id } returns mockk(relaxed = true)
            every { name } returns "Ethereum"
            every { currencySymbol } returns "ETH"
            every { rawId } returns "ethereum"
        }
    }
    val statusValue: CryptoCurrencyStatus.Value = mockk(relaxed = true) {
        every { amount } returns java.math.BigDecimal("1.0")
        every { fiatRate } returns java.math.BigDecimal("2000.00")
        every { fiatAmount } returns java.math.BigDecimal("2000.00")
    }
    val cryptoCurrencyStatus = CryptoCurrencyStatus(currency = currency, value = statusValue)
    return SwapCurrencyStatus(
        userWallet = userWallet,
        status = cryptoCurrencyStatus,
        account = account,
    )
}