package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.model.SwapProcessDataState
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class StateBuilderSwapDataTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val isBalanceHiddenProvider: Provider<Boolean> = mockk()
    private val appCurrencyProvider: Provider<AppCurrency> = mockk()
    private val isAccountsModeProvider: Provider<Boolean> = mockk()
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork = mockk()

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
        every { isGaslessFeeSupportedForNetwork(any()) } returns false

        sut = StateBuilder(
            actions = actions,
            isBalanceHiddenProvider = isBalanceHiddenProvider,
            appCurrencyProvider = appCurrencyProvider,
            isAccountsModeProvider = isAccountsModeProvider,
            isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
        )
    }

    // region createSwapInProgressState

    @Nested
    inner class CreateSwapInProgressState {

        @Test
        fun `WHEN called THEN swapButton isInProgress becomes true`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.createSwapInProgressState(baseState)

            assertThat(result.swapButton.isInProgress).isTrue()
        }

        @Test
        fun `WHEN called THEN swapButton isEnabled becomes false`() {
            val baseState = buildReadyState(coldWallet)
            // force enable the button by overriding manually
            val stateWithEnabled = baseState.copy(
                swapButton = baseState.swapButton.copy(isEnabled = true),
            )

            val result = sut.createSwapInProgressState(stateWithEnabled)

            assertThat(result.swapButton.isEnabled).isFalse()
        }

        @Test
        fun `WHEN called THEN all other fields remain unchanged`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.createSwapInProgressState(baseState)

            assertThat(result.sendCardData).isEqualTo(baseState.sendCardData)
            assertThat(result.receiveCardData).isEqualTo(baseState.receiveCardData)
            assertThat(result.fee).isEqualTo(baseState.fee)
            assertThat(result.changeCardsButtonState).isEqualTo(baseState.changeCardsButtonState)
        }
    }

    // endregion

    // region createSilentLoadState

    @Nested
    inner class CreateSilentLoadState {

        @Test
        fun `WHEN called THEN changeCardsButtonState is UPDATE_IN_PROGRESS`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.createSilentLoadState(baseState)

            assertThat(result.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.UPDATE_IN_PROGRESS)
        }

        @Test
        fun `GIVEN notifications without PermissionNeeded WHEN called THEN notifications remain unchanged`() {
            val errorNotification = SwapNotificationUM.Warning.SwapNotSupported
            val baseState = buildReadyState(coldWallet).copy(
                notifications = persistentListOf(errorNotification),
            )

            val result = sut.createSilentLoadState(baseState)

            assertThat(result.notifications).hasSize(1)
            assertThat(result.notifications[0]).isEqualTo(errorNotification)
        }

        @Test
        fun `GIVEN notifications with PermissionNeeded WHEN called THEN PermissionNeeded is removed`() {
            val permissionNeeded = SwapNotificationUM.Info.PermissionNeeded(
                onApproveClick = {},
                onLearnMoreClick = {},
            )
            val otherNotification = SwapNotificationUM.Warning.SwapNotSupported
            val baseState = buildReadyState(coldWallet).copy(
                notifications = listOf(permissionNeeded, otherNotification).toImmutableList(),
            )

            val result = sut.createSilentLoadState(baseState)

            assertThat(result.notifications).hasSize(1)
            assertThat(result.notifications[0]).isEqualTo(otherNotification)
        }
    }

    // endregion

    // region updateSwapAmount

    @Nested
    inner class UpdateSwapAmount {

        @Test
        fun `GIVEN uiState has Empty sendCard WHEN called THEN returns uiState unchanged`() {
            val loadingState = sut.createInitialLoadingState()
            val fromStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.updateSwapAmount(
                uiState = loadingState,
                amountFormatted = "1.5",
                amountRaw = "1.5",
                fromSwapCurrencyStatus = fromStatus,
                minTxAmount = null,
            )

            assertThat(result).isSameInstanceAs(loadingState)
        }

        @Test
        fun `GIVEN amount is above minTxAmount WHEN called THEN inputError is Empty`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.updateSwapAmount(
                uiState = baseState,
                amountFormatted = "2.0",
                amountRaw = "2.0",
                fromSwapCurrencyStatus = fromStatus,
                minTxAmount = BigDecimal("1.0"),
            )

            val sendCard = result.sendCardData as? SwapCardState.SwapCardData
            val inputtable = sendCard?.type as? TransactionCardType.Inputtable
            assertThat(inputtable?.inputError).isEqualTo(TransactionCardType.InputError.Empty)
        }

        @Test
        fun `GIVEN amount is below minTxAmount WHEN called THEN inputError is WrongAmount`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.updateSwapAmount(
                uiState = baseState,
                amountFormatted = "0.5",
                amountRaw = "0.5",
                fromSwapCurrencyStatus = fromStatus,
                minTxAmount = BigDecimal("1.0"),
            )

            val sendCard = result.sendCardData as? SwapCardState.SwapCardData
            val inputtable = sendCard?.type as? TransactionCardType.Inputtable
            assertThat(inputtable?.inputError).isEqualTo(TransactionCardType.InputError.WrongAmount)
        }

        @Test
        fun `GIVEN minTxAmount is null WHEN called THEN inputError is Empty`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.updateSwapAmount(
                uiState = baseState,
                amountFormatted = "0.001",
                amountRaw = "0.001",
                fromSwapCurrencyStatus = fromStatus,
                minTxAmount = null,
            )

            val sendCard = result.sendCardData as? SwapCardState.SwapCardData
            val inputtable = sendCard?.type as? TransactionCardType.Inputtable
            assertThat(inputtable?.inputError).isEqualTo(TransactionCardType.InputError.Empty)
        }

        @Test
        fun `WHEN called THEN sendCardData amountTextFieldValue text is updated`() {
            val baseState = buildReadyState(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)

            val result = sut.updateSwapAmount(
                uiState = baseState,
                amountFormatted = "3.14",
                amountRaw = "3.14",
                fromSwapCurrencyStatus = fromStatus,
                minTxAmount = null,
            )

            val sendCard = result.sendCardData as? SwapCardState.SwapCardData
            assertThat(sendCard?.amountTextFieldValue?.text).isEqualTo("3.14")
        }
    }

    // endregion

    // region updateBalanceHiddenState

    @Nested
    inner class UpdateBalanceHiddenState {

        @Test
        fun `GIVEN isBalanceHidden true WHEN called THEN sendCardData isBalanceHidden is true`() {
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)
            val baseState = sut.createInitialReadyState(
                uiStateHolder = sut.createInitialLoadingState(),
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            val result = sut.updateBalanceHiddenState(baseState, isBalanceHidden = true)

            val sendCard = result.sendCardData as? SwapCardState.SwapCardData
            assertThat(sendCard?.isBalanceHidden).isTrue()
        }

        @Test
        fun `GIVEN isBalanceHidden true WHEN called THEN receiveCardData isBalanceHidden is true`() {
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)
            val baseState = sut.createInitialReadyState(
                uiStateHolder = sut.createInitialLoadingState(),
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            val result = sut.updateBalanceHiddenState(baseState, isBalanceHidden = true)

            val receiveCard = result.receiveCardData as? SwapCardState.SwapCardData
            assertThat(receiveCard?.isBalanceHidden).isTrue()
        }

        @Test
        fun `GIVEN isBalanceHidden false WHEN called THEN both cards isBalanceHidden is false`() {
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)
            val baseState = sut.createInitialReadyState(
                uiStateHolder = sut.createInitialLoadingState(),
                emptyAmountState = emptyAmountState,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            val result = sut.updateBalanceHiddenState(baseState, isBalanceHidden = false)

            val sendCard = result.sendCardData as? SwapCardState.SwapCardData
            val receiveCard = result.receiveCardData as? SwapCardState.SwapCardData
            assertThat(sendCard?.isBalanceHidden).isFalse()
            assertThat(receiveCard?.isBalanceHidden).isFalse()
        }

        @Test
        fun `GIVEN sendCard is Empty type WHEN called THEN sendCard remains Empty type`() {
            val loadingState = sut.createInitialLoadingState()

            val result = sut.updateBalanceHiddenState(loadingState, isBalanceHidden = true)

            assertThat(result.sendCardData).isInstanceOf(SwapCardState.Empty::class.java)
        }
    }

    // endregion

    // region loadingPermissionState

    @Nested
    inner class LoadingPermissionState {

        @Test
        fun `WHEN called THEN swapButton isEnabled is false`() {
            val baseState = buildReadyState(coldWallet).copy(
                swapButton = buildReadyState(coldWallet).swapButton.copy(isEnabled = true),
            )

            val result = sut.loadingPermissionState(baseState)

            assertThat(result.swapButton.isEnabled).isFalse()
        }

        @Test
        fun `WHEN called THEN swapButton isInProgress is false`() {
            val baseState = buildReadyState(coldWallet).copy(
                swapButton = buildReadyState(coldWallet).swapButton.copy(isInProgress = true),
            )

            val result = sut.loadingPermissionState(baseState)

            assertThat(result.swapButton.isInProgress).isFalse()
        }

        @Test
        fun `GIVEN notifications without PermissionNeeded WHEN called THEN ApprovalInProgressWarning is prepended`() {
            val existingNotification = SwapNotificationUM.Warning.SwapNotSupported
            val baseState = buildReadyState(coldWallet).copy(
                notifications = persistentListOf(existingNotification),
            )

            val result = sut.loadingPermissionState(baseState)

            assertThat(result.notifications[0]).isInstanceOf(SwapNotificationUM.Error.ApprovalInProgressWarning::class.java)
        }

        @Test
        fun `GIVEN notifications with PermissionNeeded WHEN called THEN PermissionNeeded is replaced by ApprovalInProgressWarning`() {
            val permissionNeeded = SwapNotificationUM.Info.PermissionNeeded(
                onApproveClick = {},
                onLearnMoreClick = {},
            )
            val baseState = buildReadyState(coldWallet).copy(
                notifications = persistentListOf(permissionNeeded),
            )

            val result = sut.loadingPermissionState(baseState)

            assertThat(result.notifications).doesNotContain(permissionNeeded)
            assertThat(result.notifications[0]).isInstanceOf(SwapNotificationUM.Error.ApprovalInProgressWarning::class.java)
        }
    }

    // endregion

    // region dismissBottomSheet

    @Nested
    inner class DismissBottomSheet {

        @Test
        fun `GIVEN bottomSheetConfig is null WHEN called THEN bottomSheetConfig remains null`() {
            val baseState = buildReadyState(coldWallet)
            assertThat(baseState.bottomSheetConfig).isNull()

            val result = sut.dismissBottomSheet(baseState)

            assertThat(result.bottomSheetConfig).isNull()
        }

        @Test
        fun `GIVEN bottomSheetConfig is shown WHEN called THEN bottomSheetConfig isShown becomes false`() {
            val baseState = buildReadyState(coldWallet).copy(
                bottomSheetConfig = com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = {},
                    content = mockk(relaxed = true),
                ),
            )

            val result = sut.dismissBottomSheet(baseState)

            assertThat(result.bottomSheetConfig?.isShown).isFalse()
        }
    }

    // endregion

    // region addNotification

    @Nested
    inner class AddNotification {

        @Test
        fun `GIVEN a message WHEN called THEN notifications contains GenericError`() {
            val baseState = buildReadyState(coldWallet)
            val message = com.tangem.core.ui.extensions.stringReference("Something went wrong")

            val result = sut.addNotification(
                uiState = baseState,
                message = message,
                onClick = {},
            )

            assertThat(result.notifications).hasSize(1)
            assertThat(result.notifications[0]).isInstanceOf(SwapNotificationUM.Error.GenericError::class.java)
        }

        @Test
        fun `GIVEN null message WHEN called THEN notifications contains GenericError`() {
            val baseState = buildReadyState(coldWallet)

            val result = sut.addNotification(
                uiState = baseState,
                message = null,
                onClick = {},
            )

            assertThat(result.notifications).hasSize(1)
            assertThat(result.notifications[0]).isInstanceOf(SwapNotificationUM.Error.GenericError::class.java)
        }
    }

    // endregion

    // region createSuccessState

    @Nested
    inner class CreateSuccessState {

        @Test
        fun `GIVEN valid state WHEN called THEN successState is not null`() {
            val baseState = buildReadyStateWithContentProvider(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)
            val dataState = SwapProcessDataState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                selectedFee = null,
            )
            val swapTransactionState = buildSwapTransactionState()

            val result = sut.createSuccessState(
                uiState = baseState,
                swapTransactionState = swapTransactionState,
                dataState = dataState,
                onExploreClick = {},
                onStatusClick = {},
                txUrl = "https://example.com/tx/abc",
            )

            assertThat(result.successState).isNotNull()
        }

        @Test
        fun `GIVEN CEX provider WHEN called THEN shouldShowStatusButton is true`() {
            val baseState = buildReadyStateWithContentProvider(
                coldWallet,
                providerType = ExchangeProviderType.CEX,
            )
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)
            val dataState = SwapProcessDataState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                selectedFee = null,
            )
            val swapTransactionState = buildSwapTransactionState()

            val result = sut.createSuccessState(
                uiState = baseState,
                swapTransactionState = swapTransactionState,
                dataState = dataState,
                onExploreClick = {},
                onStatusClick = {},
                txUrl = "https://example.com/tx/abc",
            )

            assertThat(result.successState?.shouldShowStatusButton).isTrue()
        }

        @Test
        fun `GIVEN DEX provider WHEN called THEN shouldShowStatusButton is false`() {
            val baseState = buildReadyStateWithContentProvider(
                coldWallet,
                providerType = ExchangeProviderType.DEX,
            )
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)
            val dataState = SwapProcessDataState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                selectedFee = null,
            )
            val swapTransactionState = buildSwapTransactionState()

            val result = sut.createSuccessState(
                uiState = baseState,
                swapTransactionState = swapTransactionState,
                dataState = dataState,
                onExploreClick = {},
                onStatusClick = {},
                txUrl = "https://example.com/tx/abc",
            )

            assertThat(result.successState?.shouldShowStatusButton).isFalse()
        }

        @Test
        fun `GIVEN txUrl WHEN called THEN successState txUrl matches`() {
            val baseState = buildReadyStateWithContentProvider(coldWallet)
            val fromStatus = buildSwapCurrencyStatus(coldWallet)
            val toStatus = buildSwapCurrencyStatus(coldWallet)
            val dataState = SwapProcessDataState(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                selectedFee = null,
            )
            val swapTransactionState = buildSwapTransactionState()
            val expectedUrl = "https://etherscan.io/tx/0xabc"

            val result = sut.createSuccessState(
                uiState = baseState,
                swapTransactionState = swapTransactionState,
                dataState = dataState,
                onExploreClick = {},
                onStatusClick = {},
                txUrl = expectedUrl,
            )

            assertThat(result.successState?.txUrl).isEqualTo(expectedUrl)
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

    private fun buildReadyStateWithContentProvider(
        userWallet: UserWallet,
        providerType: ExchangeProviderType = ExchangeProviderType.DEX,
    ): SwapStateHolder {
        val baseState = buildReadyState(userWallet)
        return baseState.copy(
            providerState = ProviderState.Content(
                id = "provider-id",
                name = "TestProvider",
                type = providerType.providerName,
                iconUrl = "https://example.com/icon.png",
                subtitle = com.tangem.core.ui.extensions.stringReference("1 ETH ≈ 2000 USDT"),
                additionalBadge = ProviderState.AdditionalBadge.Empty,
                selectionType = ProviderState.SelectionType.CLICK,
                namePrefix = ProviderState.PrefixType.NONE,
                onProviderClick = {},
            ),
        )
    }

    private fun buildSwapTransactionState(): SwapTransactionState.TxSent {
        return SwapTransactionState.TxSent(
            fromAmount = "1.0 ETH",
            toAmount = "2000 USDT",
            fromAmountValue = BigDecimal("1.0"),
            toAmountValue = BigDecimal("2000"),
            txHash = "0xabc",
            timestamp = System.currentTimeMillis(),
        )
    }
}