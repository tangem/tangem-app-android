package com.tangem.features.markets.portfolio.impl.ui.state

import com.tangem.core.ui.components.token.state.TokenItemState

internal data class PortfolioTokenUM(
    val tokenItemState: TokenItemState,
    val isBalanceHidden: Boolean,
    val isQuickActionsShown: Boolean,
    val onQuickActionClick: (QuickActionUM) -> Unit,
)
