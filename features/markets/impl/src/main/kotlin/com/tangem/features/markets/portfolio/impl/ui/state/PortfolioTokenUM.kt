package com.tangem.features.markets.portfolio.impl.ui.state

import com.tangem.common.ui.account.CryptoPortfolioIconUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.collections.immutable.ImmutableList

internal sealed interface PortfolioListItem {
    val id: String
}

internal data class WalletHeader(
    override val id: String,
    val name: TextReference,
) : PortfolioListItem

internal data class AccountHeader(
    override val id: String,
    val name: TextReference,
    val icon: CryptoPortfolioIconUM,
) : PortfolioListItem

internal data class PortfolioTokenUM(
    val tokenItemState: TokenItemState,
    val walletId: UserWalletId,
    val isBalanceHidden: Boolean,
    val isQuickActionsShown: Boolean,
    val quickActions: QuickActions,
) : PortfolioListItem {
    override val id: String = tokenItemState.id

    data class QuickActions(
        val actions: ImmutableList<QuickActionUM>,
        val onQuickActionClick: (QuickActionUM) -> Unit,
        val onQuickActionLongClick: (QuickActionUM) -> Unit,
    )
}