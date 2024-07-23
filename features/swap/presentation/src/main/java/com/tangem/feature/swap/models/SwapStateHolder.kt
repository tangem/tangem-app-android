package com.tangem.feature.swap.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.ui.bottomsheet.permission.state.GiveTxPermissionState
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.models.states.FeeItemState
import com.tangem.feature.swap.models.states.ProviderState

data class SwapStateHolder(
    val sendCardData: SwapCardState,
    val receiveCardData: SwapCardState,
    val blockchainId: String, // not the same as networkId, its local id in app
    val warnings: List<SwapWarning> = emptyList(),
    val alert: SwapWarning.GenericWarning? = null,
    val changeCardsButtonState: ChangeCardsButtonState = ChangeCardsButtonState.ENABLED,
    val providerState: ProviderState,

    val fee: FeeItemState = FeeItemState.Empty,
    val permissionState: GiveTxPermissionState = GiveTxPermissionState.Empty,
    val priceImpact: PriceImpact,

    val successState: SwapSuccessStateHolder? = null,
    val selectTokenState: SwapSelectTokenStateHolder? = null,
    val bottomSheetConfig: TangemBottomSheetConfig? = null,

    val swapButton: SwapButton,
    val shouldShowMaxAmount: Boolean,
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
    val onClick: () -> Unit,
)

sealed interface TransactionCardType {

    val headerResId: Int

    data class Inputtable(
        val onAmountChanged: ((String) -> Unit),
        val onFocusChanged: ((Boolean) -> Unit),
        @StringRes override val headerResId: Int = R.string.swapping_from_title,
    ) : TransactionCardType

    data class ReadOnly(
        val showWarning: Boolean = false,
        val onWarningClick: (() -> Unit)? = null,
        @StringRes override val headerResId: Int = R.string.swapping_to_title,
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
        val title: TextReference? = null,
        val message: TextReference? = null,
        val type: GenericWarningType = GenericWarningType.OTHER,
        val onClick: () -> Unit,
    ) : SwapWarning

    data class GeneralError(val notificationConfig: NotificationConfig) : SwapWarning
    data class UnableToCoverFeeWarning(val notificationConfig: NotificationConfig) : SwapWarning
    data class GeneralWarning(val notificationConfig: NotificationConfig) : SwapWarning
    data class GeneralInformational(val notificationConfig: NotificationConfig) : SwapWarning
    data class TransactionInProgressWarning(val title: TextReference, val description: TextReference) : SwapWarning
    data class NeedReserveToCreateAccount(val notificationConfig: NotificationConfig) : SwapWarning
    data class ReduceAmount(val notificationConfig: NotificationConfig) : SwapWarning

    sealed interface Cardano : SwapWarning {
        val notificationConfig: NotificationConfig

        data class MinAdaValueCharged(override val notificationConfig: NotificationConfig) : Cardano

        data class InsufficientBalanceToTransferCoin(override val notificationConfig: NotificationConfig) : Cardano

        data class InsufficientBalanceToTransferToken(override val notificationConfig: NotificationConfig) : Cardano
    }
}

enum class GenericWarningType {
    NETWORK, OTHER
}

enum class ChangeCardsButtonState {
    ENABLED, DISABLED, UPDATE_IN_PROGRESS
}