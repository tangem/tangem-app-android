package com.tangem.features.markets.portfolio.impl.ui.state

import com.tangem.core.ui.components.token.state.TokenItemState
import kotlinx.collections.immutable.ImmutableList

internal data class PortfolioTokenUM(
    val tokenItemState: TokenItemState,
    val isBalanceHidden: Boolean,
    val isQuickActionsShown: Boolean,
    val quickActions: QuickActions,
) {

    data class QuickActions(
        val actions: ImmutableList<QuickActionUM>,
        val onQuickActionClick: (QuickActionUM) -> Unit,
        val onQuickActionLongClick: (QuickActionUM) -> Unit,
    )
}
