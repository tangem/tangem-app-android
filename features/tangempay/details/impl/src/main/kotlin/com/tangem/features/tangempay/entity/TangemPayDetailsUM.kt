package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayDetailsUM(
    val topBarConfig: TangemPayDetailsTopBarConfig,
    val pullToRefreshConfig: PullToRefreshConfig,
    val balanceBlockState: TangemPayDetailsBalanceBlockState,
    val addToWalletBlockState: AddToWalletBlockState?,
    val isBalanceHidden: Boolean,
    val addFundsEnabled: Boolean,
    val cardFrozenState: CardFrozenState,
)

internal data class TangemPayCardDetailsUM(
    val number: String,
    val expiry: String,
    val cvv: String,
    val buttonText: TextReference = TextReference.EMPTY,
    val onClick: () -> Unit = {},
    val onCopy: (String) -> Unit = {},
    val isHidden: Boolean = true,
    val isLoading: Boolean = false,
    val cardFrozenState: TangemPayCardFrozenState,
)

internal sealed class TangemPayDetailsBalanceBlockState {

    abstract val actionButtons: ImmutableList<ActionButtonConfig>

    data class Loading(
        override val actionButtons: ImmutableList<ActionButtonConfig>,
    ) : TangemPayDetailsBalanceBlockState()

    data class Content(
        override val actionButtons: ImmutableList<ActionButtonConfig>,
        val cryptoBalance: String,
        val fiatBalance: String,
        val isBalanceFlickering: Boolean,
    ) : TangemPayDetailsBalanceBlockState()

    data class Error(
        override val actionButtons: ImmutableList<ActionButtonConfig>,
    ) : TangemPayDetailsBalanceBlockState()
}

sealed class CardFrozenState {
    data object Pending : CardFrozenState()
    data class Frozen(val onUnfreeze: () -> Unit) : CardFrozenState()
    data object Unfrozen : CardFrozenState()
}

internal data class AddToWalletBlockState(
    val onClick: () -> Unit,
    val onClickClose: () -> Unit,
)