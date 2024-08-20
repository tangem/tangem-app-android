package com.tangem.features.markets.portfolio.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

internal data class PortfolioTokenUM(
    val id: String,
    val networkId: String,
    val iconUrl: String,
    val title: String,
    val subtitle: String,
    val balanceContent: BalanceContent,
    val onClick: () -> Unit,
    val onLongTap: () -> Unit,
    val isQuickActionsShown: Boolean,
    val onQuickActionClick: (QuickActionUM) -> Unit,
) {
// [REDACTED_TODO_COMMENT]
    @Immutable
    sealed class BalanceContent {
        data class TokenBalance( // TODO Add stacking (AND-7965 [Markets] Add staking info to portfolio token item)
            val balance: String,
            val tokenAmount: String,
            val hidden: Boolean,
        ) : BalanceContent()

        data class Disabled(
            val text: TextReference,
        ) : BalanceContent()

        data object Loading : BalanceContent()
    }
}
