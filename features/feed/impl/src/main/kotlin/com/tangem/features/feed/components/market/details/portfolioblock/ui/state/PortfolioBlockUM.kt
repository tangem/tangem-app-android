package com.tangem.features.feed.components.market.details.portfolioblock.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class PortfolioBlockUM {

    data object Loading : PortfolioBlockUM()
    data object Hidden : PortfolioBlockUM()

    data class AddToken(
        val tokenIcon: CurrencyIconState,
        val onClick: () -> Unit,
    ) : PortfolioBlockUM()

    data class Content(
        val totalBalance: TextReference,
        val tokensInPortfolioCount: Int,
        val tokenIcon: CurrencyIconState,
        val tokenName: String,
        val tokenSymbol: String,
        val isBalanceHidden: Boolean,
        val onClick: () -> Unit,
    ) : PortfolioBlockUM()
}