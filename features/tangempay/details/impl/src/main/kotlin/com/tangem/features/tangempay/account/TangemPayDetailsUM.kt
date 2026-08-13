package com.tangem.features.tangempay.account

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.pay.TangemPayCardState
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayDetailsUM(
    val topBarConfig: TangemPayDetailsTopBarConfig,
    val pullToRefreshConfig: PullToRefreshConfig,
    val balanceBlockState: TangemPayDetailsBalanceBlockState,
    val isBalanceHidden: Boolean,
    val errorNotificationConfig: NotificationConfig?,
    val accountDeactivatedNotificationConfig: NotificationConfig?,
    val cashbackBlockState: CashbackBlockUM? = null,
)

@Immutable
internal sealed interface CashbackBlockUM {

    data class Widget(
        val title: TextReference,
        val subtitle: TextReference,
        val onClick: () -> Unit,
    ) : CashbackBlockUM

    data class DeactivatedBanner(
        val onGotIt: () -> Unit,
    ) : CashbackBlockUM
}

internal enum class TangemPayAction { AddFunds, Withdraw }

@Immutable
internal data class TangemPayActionButtonUM(
    val action: TangemPayAction,
    val config: ActionButtonConfig,
)

@Immutable
internal sealed class TangemPayDetailsBalanceBlockState {

    abstract val actionButtons: ImmutableList<TangemPayActionButtonUM>
    abstract val cardsBlockState: CardsBlockState?

    data class Loading(
        override val actionButtons: ImmutableList<TangemPayActionButtonUM>,
        override val cardsBlockState: CardsBlockState?,
    ) : TangemPayDetailsBalanceBlockState()

    data class Content(
        override val actionButtons: ImmutableList<TangemPayActionButtonUM>,
        override val cardsBlockState: CardsBlockState?,
        val fiatBalance: TextReference,
        val isBalanceFlickering: Boolean,
        val isNegative: Boolean,
        val isInactive: Boolean,
        val isMuted: Boolean = false,
    ) : TangemPayDetailsBalanceBlockState()

    data class Error(
        override val actionButtons: ImmutableList<TangemPayActionButtonUM>,
        override val cardsBlockState: CardsBlockState?,
    ) : TangemPayDetailsBalanceBlockState()

    data class CardsBlockState(
        val cards: ImmutableList<Card>,
        val onAddCardClick: () -> Unit,
        val isAddCardEnabled: Boolean,
        val progressBanner: CardsProgressBannerUM? = null,
    )

    data class Card(
        val lastDigits: String,
        val imageUrl: String?,
        val onClick: () -> Unit,
        val state: TangemPayCardUiState,
        val isFrozen: Boolean,
        val isEnabled: Boolean,
    )
}

internal enum class TangemPayCardUiState {
    Active,
    InProgress,
}

internal enum class CardsProgressBannerUM {
    Issuing,
    Reissuing,
}

internal fun TangemPayCardState.toUiState(): TangemPayCardUiState = when (this) {
    TangemPayCardState.Active -> TangemPayCardUiState.Active
    TangemPayCardState.Issuing,
    TangemPayCardState.Reissuing,
    TangemPayCardState.Closing,
    -> TangemPayCardUiState.InProgress
}