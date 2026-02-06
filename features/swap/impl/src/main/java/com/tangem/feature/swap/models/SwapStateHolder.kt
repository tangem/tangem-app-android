package com.tangem.feature.swap.models

import androidx.annotation.DrawableRes
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.bottomsheet.permission.state.GiveTxPermissionState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.swapStoriesScreen.SwapStoriesUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.models.states.FeeItemState
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.models.states.events.SwapEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class SwapStateHolder(
    val sendCardData: SwapCardState,
    val receiveCardData: SwapCardState,
    val blockchainId: String, // not the same as networkId, its local id in app
    val notifications: ImmutableList<NotificationUM> = persistentListOf(),
    val isInsufficientFunds: Boolean,
    val event: StateEvent<SwapEvent> = consumedEvent(),
    val changeCardsButtonState: ChangeCardsButtonState,
    val providerState: ProviderState,

    val fee: FeeItemState = FeeItemState.Empty,
    val permissionState: GiveTxPermissionState = GiveTxPermissionState.Empty,
    val priceImpact: PriceImpact,

    val successState: SwapSuccessStateHolder? = null,
    val selectTokenState: SwapSelectTokenStateHolder? = null,
    val bottomSheetConfig: TangemBottomSheetConfig? = null,
    val storiesConfig: SwapStoriesUM? = null,

    val swapButton: SwapButton,
    val shouldShowMaxAmount: Boolean,
    val tosState: TosState? = null,

    val onRefresh: () -> Unit,
    val onBackClicked: () -> Unit,
    val onChangeCardsClicked: () -> Unit,
    val onSelectTokenClick: (() -> Unit),
    val onSuccess: (() -> Unit),
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
    @DrawableRes val walletInteractionIcon: Int?,
    val isEnabled: Boolean,
    val isInProgress: Boolean = false,
    val isHoldToConfirm: Boolean = false,
    val onClick: () -> Unit,
)

sealed interface TransactionCardType {

    val accountTitleUM: AccountTitleUM?
    val inputError: InputError

    data class Inputtable(
        val onAmountChanged: ((String) -> Unit),
        val onFocusChanged: ((Boolean) -> Unit),
        override val inputError: InputError,
        override val accountTitleUM: AccountTitleUM?,
    ) : TransactionCardType

    data class ReadOnly(
        val shouldShowWarning: Boolean = false,
        val onWarningClick: (() -> Unit)? = null,
        override val inputError: InputError = InputError.Empty,
        override val accountTitleUM: AccountTitleUM? = null,
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