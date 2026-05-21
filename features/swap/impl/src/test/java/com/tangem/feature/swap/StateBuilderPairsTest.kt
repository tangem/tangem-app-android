package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRouter
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
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

internal class StateBuilderPairsTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val isBalanceHiddenProvider: Provider<Boolean> = mockk()
    private val appCurrencyProvider: Provider<AppCurrency> = mockk()
    private val isAccountsModeProvider: Provider<Boolean> = mockk()
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork = mockk()
    private val swapFeatureToggles: SwapFeatureToggles = mockk(relaxed = true)
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
        zeroAmountEquivalent = stringReference("$0.00"),
    )

    @BeforeEach
    fun setup() {
        every { isBalanceHiddenProvider() } returns false
        every { appCurrencyProvider() } returns AppCurrency.Default
        every { isAccountsModeProvider() } returns false

        sut = StateBuilder(
            actions = actions,
            isBalanceHiddenProvider = isBalanceHiddenProvider,
            appCurrencyProvider = appCurrencyProvider,
            isAccountsModeProvider = isAccountsModeProvider,
            isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
            swapFeatureToggles = swapFeatureToggles,
            appRouter = appRouter,
        )
    }

    // region createSwapNotSupportedState

    @Nested
    inner class CreateSwapNotSupportedState {

        @Test
        fun `GIVEN uiState sendCardData is not SwapCardData WHEN called THEN returns uiState unchanged`() {
            val loadingState = sut.createInitialLoadingState()
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createSwapNotSupportedState(
                uiStateHolder = loadingState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result).isSameInstanceAs(loadingState)
        }

        @Test
        fun `GIVEN valid SwapCardData state WHEN called THEN swapButton is disabled`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createSwapNotSupportedState(
                uiStateHolder = baseState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.swapButton.isEnabled).isFalse()
        }

        @Test
        fun `GIVEN valid SwapCardData state WHEN called THEN changeCardsButtonState is DISABLED`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createSwapNotSupportedState(
                uiStateHolder = baseState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.DISABLED)
        }

        @Test
        fun `GIVEN valid state WHEN called THEN notifications contain SwapNotSupported warning`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createSwapNotSupportedState(
                uiStateHolder = baseState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.notifications).hasSize(1)
            assertThat(result.notifications[0]).isInstanceOf(SwapNotificationUM.Warning.SwapNotSupported::class.java)
        }

        @Test
        fun `GIVEN valid state WHEN called THEN fee is Empty`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createSwapNotSupportedState(
                uiStateHolder = baseState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.fee).isInstanceOf(FeeItemState.Empty::class.java)
        }

        @Test
        fun `GIVEN valid state WHEN called THEN providerState is Empty`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createSwapNotSupportedState(
                uiStateHolder = baseState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.providerState).isInstanceOf(ProviderState.Empty::class.java)
        }

        @Test
        fun `GIVEN valid state with cold wallet WHEN called THEN swapButton isHoldToConfirm is false`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createSwapNotSupportedState(
                uiStateHolder = baseState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.swapButton.isHoldToConfirm).isFalse()
        }

        @Test
        fun `GIVEN valid state with hot wallet WHEN called THEN swapButton isHoldToConfirm is true`() {
            val baseState = buildReadyState(hotWallet)
            val fromStatus = buildSwapCurrencyStatus(hotWallet)
            val toStatus = buildSwapCurrencyStatus(hotWallet)

            val result = sut.createSwapNotSupportedState(
                uiStateHolder = baseState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            assertThat(result.swapButton.isHoldToConfirm).isTrue()
        }

        @Test
        fun `GIVEN valid state WHEN called THEN sendCardData type is ReadOnly`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.createSwapNotSupportedState(
                uiStateHolder = baseState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            val sendCard = result.sendCardData as? SwapCardState.SwapCardData
            assertThat(sendCard?.type).isInstanceOf(TransactionCardType.ReadOnly::class.java)
        }
    }

    // endregion

    // region updateCurrenciesState

    @Nested
    inner class UpdateCurrenciesState {

        @Test
        fun `GIVEN both currencies null WHEN called THEN sendCard is Empty`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.updateCurrenciesState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
                toSwapCurrencyStatus = null,
                shouldResetAmount = false,
            )

            assertThat(result.sendCardData).isInstanceOf(SwapCardState.Empty::class.java)
        }

        @Test
        fun `GIVEN both currencies non-null WHEN called THEN sendCard is SwapCardData`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.updateCurrenciesState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                shouldResetAmount = false,
            )

            assertThat(result.sendCardData).isInstanceOf(SwapCardState.SwapCardData::class.java)
        }

        @Test
        fun `WHEN called THEN notifications is cleared`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.updateCurrenciesState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
                toSwapCurrencyStatus = null,
                shouldResetAmount = false,
            )

            assertThat(result.notifications).isEmpty()
        }

        @Test
        fun `WHEN called THEN isInsufficientFunds is false`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.updateCurrenciesState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
                toSwapCurrencyStatus = null,
                shouldResetAmount = false,
            )

            assertThat(result.isInsufficientFunds).isFalse()
        }

        @Test
        fun `WHEN called THEN fee is Empty`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.updateCurrenciesState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
                toSwapCurrencyStatus = null,
                shouldResetAmount = false,
            )

            assertThat(result.fee).isInstanceOf(FeeItemState.Empty::class.java)
        }

        @Test
        fun `WHEN called THEN changeCardsButtonState is ENABLED`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.updateCurrenciesState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = null,
                toSwapCurrencyStatus = null,
                shouldResetAmount = false,
            )

            assertThat(result.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.ENABLED)
        }

        @Test
        fun `GIVEN hot wallet fromStatus WHEN called THEN swapButton isHoldToConfirm is true`() {
            val baseState = buildReadyState(hotWallet)
            val fromStatus = buildSwapCurrencyStatus(hotWallet)

            val result = sut.updateCurrenciesState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = null,
                shouldResetAmount = false,
            )

            assertThat(result.swapButton.isHoldToConfirm).isTrue()
        }

        @Test
        fun `GIVEN toSwapCurrencyStatus is null WHEN called THEN sendCard isEnabled is false`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.updateCurrenciesState(
                uiStateHolder = baseState,
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = null,
                shouldResetAmount = false,
            )

            val sendCard = result.sendCardData as? SwapCardState.SwapCardData
            val inputtable = sendCard?.type as? TransactionCardType.Inputtable
            assertThat(inputtable?.isEnabled).isFalse()
        }
    }

    // endregion

    // region updateCurrencyBalanceStatus

    @Nested
    inner class UpdateCurrencyBalanceStatus {

        @Test
        fun `GIVEN balance hidden flag true WHEN called THEN sendCardData isBalanceHidden is true`() {
            every { isBalanceHiddenProvider() } returns true
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.updateCurrencyBalanceStatus(
                uiState = baseState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                emptyAmountState = emptyAmountState,
            )

            val sendCard = result.sendCardData as? SwapCardState.SwapCardData
            assertThat(sendCard?.isBalanceHidden).isTrue()
        }

        @Test
        fun `GIVEN balance hidden flag false WHEN called THEN sendCardData isBalanceHidden is false`() {
            every { isBalanceHiddenProvider() } returns false
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.updateCurrencyBalanceStatus(
                uiState = baseState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                emptyAmountState = emptyAmountState,
            )

            val sendCard = result.sendCardData as? SwapCardState.SwapCardData
            assertThat(sendCard?.isBalanceHidden).isFalse()
        }

        @Test
        fun `GIVEN fromStatus null WHEN called THEN sendCard becomes Empty`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.updateCurrencyBalanceStatus(
                uiState = baseState,
                fromSwapCurrencyStatus = null,
                toSwapCurrencyStatus = null,
                emptyAmountState = emptyAmountState,
            )

            assertThat(result.sendCardData).isInstanceOf(SwapCardState.Empty::class.java)
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
}