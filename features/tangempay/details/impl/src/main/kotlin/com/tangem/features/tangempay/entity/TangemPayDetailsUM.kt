package com.tangem.features.tangempay.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import kotlinx.collections.immutable.ImmutableList

internal enum class CardDataType {
    Number, Expiry, CVV
}

internal data class TangemPayDetailsUM(
    val topBarConfig: TangemPayDetailsTopBarConfig,
    val pullToRefreshConfig: PullToRefreshConfig,
    val balanceBlockState: TangemPayDetailsBalanceBlockState,
    val addToWalletBlockState: AddToWalletBlockState?,
    val isBalanceHidden: Boolean,
    val errorNotificationConfig: NotificationConfig?,
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
    val isActionsAvailable: Boolean = false,
    val shouldShowCardDetailsButtonOnCard: Boolean = false,
)

@Immutable
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
        val isSubmitEnabled: Boolean,
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

@Immutable
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
        val fiatBalance: TextReference,
        val isBalanceFlickering: Boolean,
    ) : TangemPayDetailsBalanceBlockState()

    data class Error(
        override val actionButtons: ImmutableList<ActionButtonConfig>,
        override val cardsBlockState: CardsBlockState?,
    ) : TangemPayDetailsBalanceBlockState()

    data class CardsBlockState(val cards: ImmutableList<Card>, val onAddCardClick: () -> Unit)
    data class Card(
        val lastDigits: String,
        val onClick: () -> Unit,
        val isReissuing: Boolean,
        val isFrozen: Boolean,
        val isEnabled: Boolean,
    )
}

internal data class AddToWalletBlockState(
    val onClick: () -> Unit,
    val onClickClose: () -> Unit,
)