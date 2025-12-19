package com.tangem.feature.swap.models

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class SwapSelectTokenStateHolder(
    val availableTokens: ImmutableList<TokenToSelectState>,
    val unavailableTokens: ImmutableList<TokenToSelectState>,
    val tokensListData: TokenListUMData,
    val isBalanceHidden: Boolean,
    val isAfterSearch: Boolean,
    val onSearchEntered: (String) -> Unit,
    val onTokenSelected: (String) -> Unit,
)

internal sealed class TokenToSelectState {

    data class Title(val title: TextReference) : TokenToSelectState()

    data class TokenToSelect(
        val id: String,
        val name: String,
        val symbol: String,
        val tokenIcon: CurrencyIconState,
        val isAvailable: Boolean = true,
        val addedTokenBalanceData: TokenBalanceData? = null,
    ) : TokenToSelectState()
}

internal data class TokenBalanceData(
    val amount: String?,
    val amountEquivalent: String?,
    val isBalanceHidden: Boolean,
)

internal sealed interface TokenListUMData {

    val tokensList: ImmutableList<TokensListItemUM>

    data class AccountList(
        override val tokensList: ImmutableList<TokensListItemUM.Portfolio>,
    ) : TokenListUMData

    data class TokenList(
        override val tokensList: ImmutableList<TokensListItemUM>,
    ) : TokenListUMData

    data object EmptyList : TokenListUMData {
        override val tokensList: ImmutableList<TokensListItemUM> = persistentListOf()
    }
}