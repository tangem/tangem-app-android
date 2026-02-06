package com.tangem.features.feed.components.market.details.portfolio.add.impl.ui.state

import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.features.feed.components.market.details.portfolio.impl.ui.state.PortfolioTokenUM

internal data class TokenActionsUM(
    val token: TokenItemState,
    val quickActions: PortfolioTokenUM.QuickActions,
    val onLaterClick: () -> Unit,
)