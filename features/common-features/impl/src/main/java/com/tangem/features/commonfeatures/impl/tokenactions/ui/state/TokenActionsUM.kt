package com.tangem.features.commonfeatures.impl.tokenactions.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.action.QuickActions
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.TextReference

internal data class TokenActionsUM(
    val token: TokenItemState,
    val quickActions: QuickActions,
    val bottomActionText: TextReference?,
    val onBottomActionClick: () -> Unit,
    val isBalancesHidden: Boolean = false,
    val portfolioBadge: PortfolioBadgeUM = PortfolioBadgeUM.None,
    val isCompact: Boolean = false,
)

@Immutable
internal sealed interface PortfolioBadgeUM {
    data class Account(val badge: TangemBadgeUM) : PortfolioBadgeUM
    data class Wallet(val name: TextReference, val deviceIcon: DeviceIconUM) : PortfolioBadgeUM
    data object None : PortfolioBadgeUM
}