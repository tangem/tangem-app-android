package com.tangem.features.markets.portfolio.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface PortfolioListItem {
    val id: String
}

internal data class WalletHeader(
    override val id: String,
    val name: TextReference,
) : PortfolioListItem

internal data class PortfolioHeader(
    override val id: String,
    val state: AccountTitleUM,
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