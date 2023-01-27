package com.tangem.feature.swap.models

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
    val onSelectTokenClick: (() -> Unit)? = null,
    val onSuccess: (() -> Unit)? = null,
    val onMaxAmountSelected: (() -> Unit)? = null,
    val onShowPermissionBottomSheet: () -> Unit = {},
    val onCancelPermissionBottomSheet: () -> Unit = {},
)

data class SwapCardData(
    val type: TransactionCardType,
    val amount: String?,
    val amountEquivalent: String?,
    val coinId: String?,
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

sealed interface FeeState {

    object Empty : FeeState

    data class Loaded(
        val fee: String = "",
    ) : FeeState

    object Loading : FeeState

    data class NotEnoughFundsWarning(val fee: String) : FeeState
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
    data class GenericWarning(val message: String?, val onClick: () -> Unit) : SwapWarning
    // data class RateExpired(val onClick: () -> Unit) : SwapWarning
    // object HighPriceImpact : SwapWarning
}
