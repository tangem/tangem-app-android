package com.tangem.features.feed.components.market.details.portfolioblock.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface PortfolioBlockUM {

    data object Hidden : PortfolioBlockUM

    data class AddToken(
        val tokenIcon: CurrencyIconState,
        val onAddClick: () -> Unit,
    ) : PortfolioBlockUM

    data class Content(
        val totalBalance: TextReference,
        val tokensInPortfolioCount: Int,
        val tokenIcon: CurrencyIconState,
        val tokenName: String,
        val tokenSymbol: String,
        val isBalanceHidden: Boolean,
        /** `null` leaves the row inert — a region-restricted asset must expose no route to trading it. */
        val onRowClick: (() -> Unit)?,
        val onAddFundsClick: (() -> Unit)?,
    ) : PortfolioBlockUM

    data class Unsupported(
        val tokenIcon: CurrencyIconState,
    ) : PortfolioBlockUM
}