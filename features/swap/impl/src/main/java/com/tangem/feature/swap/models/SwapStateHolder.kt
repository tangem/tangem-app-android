package com.tangem.feature.swap.models

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.swap.domain.models.domain.SwapUIMode
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.models.states.ProviderState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class SwapStateHolder(
    val sendCardData: SwapCardState,
    val receiveCardData: SwapCardState,
    val notifications: ImmutableList<NotificationUM> = persistentListOf(),
    val isInsufficientFunds: Boolean,
    val changeCardsButtonState: ChangeCardsButtonState,
    val providerState: ProviderState,

    val permissionUM: SwapPermissionUM = SwapPermissionUM.Empty,
    val priceImpact: PriceImpact,

    val successState: SwapSuccessStateHolder? = null,
    val bottomSheetConfig: TangemBottomSheetConfig? = null,
    val swapButton: SwapButton,
    val shouldShowMaxAmount: Boolean,
    val tosState: TosState? = null,
    val swapUIMode: SwapUIMode = SwapUIMode.Detailed,

    val onRefresh: () -> Unit,
    val onBackClicked: () -> Unit,
    val onChangeCardsClicked: () -> Unit,
    val onSelectTokenClick: ((TokenSelectionDirection) -> Unit),
    val onSuccess: (() -> Unit),
    val onMaxAmountSelected: (() -> Unit)? = null,
    val onShowPermissionBottomSheet: () -> Unit = {},
)

@Immutable
sealed class SwapCardState {

    abstract val type: TransactionCardType

    data class SwapCardData(
        override val type: TransactionCardType,
        val currencyIconState: CurrencyIconState,
        val tokenSymbol: TextReference,
        val amountEquivalent: TextReference?,
        val amountTextFieldValue: TextFieldValue?,
        val balance: String,
        val isBalanceHidden: Boolean,
    ) : SwapCardState()

    data class Empty(
        override val type: TransactionCardType,
        val amountEquivalent: TextReference,
        val amountTextFieldValue: TextFieldValue?,
    ) : SwapCardState()

    data class Loading(
        override val type: TransactionCardType,
    ) : SwapCardState()
}

data class SwapButton(
    @DrawableRes val walletInteractionIcon: Int?,
    val isEnabled: Boolean,
    val mode: Mode = Mode.SWAP,
    val isHoldToConfirm: Boolean = false,
    val onClick: () -> Unit,
) {
    enum class Mode {
        SWAP_PROGRESSING,
        SWAP,
        TRANSFER,
        TRANSFER_PROGRESSING,
    }

    val isInProgress
        get() = mode == Mode.SWAP_PROGRESSING || mode == Mode.TRANSFER_PROGRESSING
}

@Immutable
sealed interface TransactionCardType {

    val accountTitleUM: AccountTitleUM
    val inputError: InputError

    data class Inputtable(
        val onAmountChanged: ((String) -> Unit),
        val onFocusChanged: ((Boolean) -> Unit),
        override val inputError: InputError,
        override val accountTitleUM: AccountTitleUM,
        val isEnabled: Boolean,
    ) : TransactionCardType

    data class ReadOnly(
        val shouldShowWarning: Boolean = false,
        val onWarningClick: (() -> Unit)? = null,
        override val inputError: InputError = InputError.Empty,
        override val accountTitleUM: AccountTitleUM,
    ) : TransactionCardType

    sealed interface InputError {
        data object Empty : InputError
        data object InsufficientFunds : InputError
        data object WrongAmount : InputError
    }
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

enum class ChangeCardsButtonState {
    ENABLED, DISABLED, UPDATE_IN_PROGRESS
}

sealed class SwapPermissionUM {

    data class PermissionRequired(
        val isResetApproval: Boolean,
        val spenderAddress: String,
    ) : SwapPermissionUM()

    object Empty : SwapPermissionUM()
}