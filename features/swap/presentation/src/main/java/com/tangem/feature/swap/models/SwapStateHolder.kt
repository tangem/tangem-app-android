package com.tangem.feature.swap.models

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.states.Item
import com.tangem.core.ui.components.states.SelectableItemsState
import com.tangem.feature.swap.domain.models.ui.TxFee

data class SwapStateHolder(
    val sendCardData: SwapCardData,
    val receiveCardData: SwapCardData,
    val networkCurrency: String,
    val networkId: String,
    val blockchainId: String, // not the same as networkId, its local id in app
    val fee: FeeState = FeeState.Empty,
    val warnings: List<SwapWarning> = emptyList(),
    val alert: SwapWarning.GenericWarning? = null,
    val updateInProgress: Boolean = false,

    val permissionState: SwapPermissionState = SwapPermissionState.Empty,
    val successState: SwapSuccessStateHolder? = null,
    val selectTokenState: SwapSelectTokenStateHolder? = null,

    val swapButton: SwapButton,

    val onRefresh: () -> Unit,
    val onBackClicked: () -> Unit,
    val onChangeCardsClicked: () -> Unit,
    val onSearchFocusChange: (Boolean) -> Unit,
    val onSelectTokenClick: (() -> Unit)? = null,
    val onSuccess: (() -> Unit)? = null,
    val onMaxAmountSelected: (() -> Unit)? = null,
    val onShowPermissionBottomSheet: () -> Unit = {},
    val onCancelPermissionBottomSheet: () -> Unit = {},
)

data class SwapCardData(
    val type: TransactionCardType,
    val amountEquivalent: String?,
    val coinId: String?,
    val amountTextFieldValue: TextFieldValue?,
    val tokenIconUrl: String,
    val tokenCurrency: String,
    val balance: String,
    val isNotNativeToken: Boolean,
    val canSelectAnotherToken: Boolean = false,
)

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

    data class SendCard(
        val onAmountChanged: ((String) -> Unit),
        val onFocusChanged: ((Boolean) -> Unit),
    ) : TransactionCardType

    data class ReceiveCard(
        val highPriceImpact: String? = null,
    ) : TransactionCardType
}

sealed interface SwapWarning {
    data class PermissionNeeded(val tokenCurrency: String) : SwapWarning
    object InsufficientFunds : SwapWarning
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
    data class HighPriceImpact(val priceImpact: Int) : SwapWarning
}

enum class GenericWarningType {
    NETWORK, OTHER
}