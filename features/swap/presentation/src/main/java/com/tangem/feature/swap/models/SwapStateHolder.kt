package com.tangem.feature.swap.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.components.states.Item
import com.tangem.core.ui.components.states.SelectableItemsState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.domain.models.ui.TxFee
import com.tangem.feature.swap.models.states.FeeItemState
import com.tangem.feature.swap.models.states.ProviderState

data class SwapStateHolder(
    val sendCardData: SwapCardState,
    val receiveCardData: SwapCardState,
    val networkCurrency: String,
    val blockchainId: String, // not the same as networkId, its local id in app
    val warnings: List<SwapWarning> = emptyList(),
    val alert: SwapWarning.GenericWarning? = null,
    val changeCardsButtonState: ChangeCardsButtonState = ChangeCardsButtonState.ENABLED,
    val providerState: ProviderState,

    val fee: FeeItemState = FeeItemState.Empty,
    val permissionState: SwapPermissionState = SwapPermissionState.Empty,
    val priceImpact: PriceImpact,

    val successState: SwapSuccessStateHolder? = null,
    val selectTokenState: SwapSelectTokenStateHolder? = null,
    val bottomSheetConfig: TangemBottomSheetConfig? = null,

    val swapButton: SwapButton,
    val tosState: TosState? = null,

    val onRefresh: () -> Unit,
    val onBackClicked: () -> Unit,
    val onChangeCardsClicked: () -> Unit,
    val onSelectTokenClick: (() -> Unit)? = null,
    val onSuccess: (() -> Unit)? = null,
    val onMaxAmountSelected: (() -> Unit)? = null,
    val onShowPermissionBottomSheet: () -> Unit = {},
)

sealed class SwapCardState {

    data class SwapCardData(
        @DrawableRes val networkIconRes: Int?,
        val type: TransactionCardType,
        val amountEquivalent: String?,
        val token: CryptoCurrencyStatus?,
        val coinId: String?,
        val amountTextFieldValue: TextFieldValue?,
        val tokenIconUrl: String?,
        val tokenCurrency: String,
        val balance: String,
        val isBalanceHidden: Boolean,
        val isNotNativeToken: Boolean,
        val canSelectAnotherToken: Boolean = false,
    ) : SwapCardState()

    data class Empty(
        val type: TransactionCardType,
        val amountEquivalent: String?,
        val amountTextFieldValue: TextFieldValue?,
        val canSelectAnotherToken: Boolean = false,
    ) : SwapCardState()
}

data class SwapButton(
    val enabled: Boolean,
    val loading: Boolean = false,
    val onClick: () -> Unit,
)

sealed class FeeState(open val tangemFee: Double) {

    object Empty : FeeState(0.0)

    data class Loaded(
        override val tangemFee: Double,
        override val state: SelectableItemsState<TxFee>?,
        val onSelectItem: (Item<TxFee>) -> Unit,
    ) : FeeState(tangemFee), FeeSelectState

    object Loading : FeeState(0.0)

    data class NotEnoughFundsWarning(
        override val tangemFee: Double,
        override val state: SelectableItemsState<TxFee>?,
        val onSelectItem: (Item<TxFee>) -> Unit,
    ) : FeeState(tangemFee), FeeSelectState
}

sealed interface FeeSelectState {
    val state: SelectableItemsState<TxFee>?
}

sealed interface TransactionCardType {

    val headerResId: Int

    data class Inputtable(
        val onAmountChanged: ((String) -> Unit),
        val onFocusChanged: ((Boolean) -> Unit),
        @StringRes override val headerResId: Int = R.string.exchange_send_view_header,
    ) : TransactionCardType

    data class ReadOnly(
        val highPriceImpact: String? = null,
        @StringRes override val headerResId: Int = R.string.exchange_receive_view_header,
    ) : TransactionCardType
}

data class TosState(
    val tosLink: LegalState?,
    val policyLink: LegalState?,
)

data class LegalState(
    val title: TextReference,
    val link: String,
    val onClick: (String) -> Unit,
)

sealed interface SwapWarning {
    data class PermissionNeeded(val notificationConfig: NotificationConfig) : SwapWarning
    object InsufficientFunds : SwapWarning
    data class NoAvailableTokensToSwap(val notificationConfig: NotificationConfig) : SwapWarning
    data class GenericWarning(
        val message: String? = null,
        val type: GenericWarningType = GenericWarningType.OTHER,
        val shouldWrapMessage: Boolean = false,
        val onClick: () -> Unit,
    ) : SwapWarning
    // data class RateExpired(val onClick: () -> Unit) : SwapWarning
    /**
     * High price impact warning
     *
     * @property priceImpact in format = 10 (means 10%)
     */
    data class HighPriceImpact(val priceImpact: Int, val notificationConfig: NotificationConfig) : SwapWarning
    data class TooSmallAmountWarning(val notificationConfig: NotificationConfig) : SwapWarning
    data class UnableToCoverFeeWarning(val notificationConfig: NotificationConfig) : SwapWarning
    data class GeneralWarning(val notificationConfig: NotificationConfig) : SwapWarning
    data class TransactionInProgressWarning(val title: TextReference, val description: TextReference) : SwapWarning
}

enum class GenericWarningType {
    NETWORK, OTHER
}

enum class ChangeCardsButtonState {
    ENABLED, DISABLED, UPDATE_IN_PROGRESS
}