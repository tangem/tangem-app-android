package com.tangem.features.tangempay.entity

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.features.tangempay.model.CardDataType
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayDetailsUM(
    val topBarConfig: TangemPayDetailsTopBarConfig,
    val pullToRefreshConfig: PullToRefreshConfig,
    val balanceBlockState: TangemPayDetailsBalanceBlockState,
    val addToWalletBlockState: AddToWalletBlockState?,
    val isBalanceHidden: Boolean,
    val addFundsEnabled: Boolean,
    val accountDeactivatedNotificationConfig: NotificationConfig?,
)

internal data class TangemPayCardDetailsUM(
    val number: String,
    val numberShort: String,
    val expiry: String,
    val cvv: String,
    val buttonText: TextReference = TextReference.EMPTY,
    val onClick: () -> Unit = {},
    val onCopy: (String, CardDataType) -> Unit,
    val isHidden: Boolean = true,
    val isLoading: Boolean = false,
    val cardFrozenState: TangemPayCardFrozenState,
    val displayNameState: DisplayNameState?,
    val isActive: Boolean = true,
)

internal sealed interface DisplayNameState {

    val displayName: String

    data class Display(
        override val displayName: String,
        val onClick: () -> Unit,
        val isEditingEnabled: Boolean,
    ) : DisplayNameState

    data class Editing(
        override val displayName: String,
        val editingValue: TextFieldValue,
        val onValueChanged: (TextFieldValue) -> Unit,
        val onSubmit: () -> Unit,
        val onDismiss: () -> Unit,
    ) : DisplayNameState

    fun copySealed(displayName: String): DisplayNameState {
        return when (this) {
            is Display -> copy(displayName = displayName)
            is Editing -> copy(displayName = displayName)
        }
    }
}

internal sealed class TangemPayDetailsBalanceBlockState {

    abstract val actionButtons: ImmutableList<ActionButtonConfig>
    abstract val cardsBlockState: CardsBlockState?

    data class Loading(
        override val actionButtons: ImmutableList<ActionButtonConfig>,
        override val cardsBlockState: CardsBlockState?,
    ) : TangemPayDetailsBalanceBlockState()

    data class Content(
        override val actionButtons: ImmutableList<ActionButtonConfig>,
        override val cardsBlockState: CardsBlockState?,
        val fiatBalance: String,
        val isBalanceFlickering: Boolean,
    ) : TangemPayDetailsBalanceBlockState()

    data class Error(
        override val actionButtons: ImmutableList<ActionButtonConfig>,
        override val cardsBlockState: CardsBlockState?,
    ) : TangemPayDetailsBalanceBlockState()

    data class CardsBlockState(val cards: ImmutableList<Card>, val onAddCardClick: () -> Unit)
    data class Card(val lastDigits: String, val onClick: () -> Unit)
}

internal data class AddToWalletBlockState(
    val onClick: () -> Unit,
    val onClickClose: () -> Unit,
)