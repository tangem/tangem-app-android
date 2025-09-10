package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayDetailsUM(
    val topBarConfig: TangemPayDetailsTopBarConfig,
    val pullToRefreshConfig: PullToRefreshConfig,
    val balanceBlockState: TangemPayDetailsBalanceBlockState,
    val cardDetailsUM: TangemPayCardDetailsUM,
    val isBalanceHidden: Boolean,
)

data class TangemPayCardDetailsUM(val number: String, val expiry: String, val cvv: String, val onReveal: () -> Unit)

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