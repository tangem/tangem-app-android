package com.tangem.features.commonfeatures.impl.addtoportfolio.ui.state

import com.tangem.common.ui.markets.action.QuickActions
import com.tangem.core.ui.components.token.state.TokenItemState

internal data class TokenActionsUM(
    val token: TokenItemState,
    val quickActions: QuickActions,
    val onLaterClick: () -> Unit,
)