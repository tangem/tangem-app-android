package com.tangem.feature.swap.models

import androidx.annotation.DrawableRes
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.models.states.FeeItemState
import com.tangem.feature.swap.models.states.ProviderState

data class SwapStateHolder(
    val sendCardData: SwapCardState,
    val receiveCardData: SwapCardState,
    val networkCurrency: String,
    val blockchainId: String, // not the same as networkId, its local id in app
    val warnings: List<SwapWarning> = emptyList(),
    val alert: SwapWarning.GenericWarning? = null,
    val updateInProgress: Boolean = false,
    val providerState: ProviderState,

    val fee: FeeItemState = FeeItemState.Empty,
    val permissionState: SwapPermissionState = SwapPermissionState.Empty,

    val successState: SwapSuccessStateHolder? = null,
    val selectTokenState: SwapSelectTokenStateHolder? = null,
    val bottomSheetConfig: TangemBottomSheetConfig? = null,

    val swapButton: SwapButton,

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
}

enum class GenericWarningType {
    NETWORK, OTHER
}