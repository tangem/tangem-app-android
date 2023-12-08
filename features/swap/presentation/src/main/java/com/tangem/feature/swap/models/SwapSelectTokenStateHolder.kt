package com.tangem.feature.swap.models

import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

data class SwapSelectTokenStateHolder(
    val availableTokens: ImmutableList<TokenToSelectState>,
    val unavailableTokens: ImmutableList<TokenToSelectState>,
    val onSearchEntered: (String) -> Unit,
    val onTokenSelected: (String) -> Unit,
)

sealed class TokenToSelectState {

    data class Title(val title: TextReference) : TokenToSelectState()

    data class TokenToSelect(
        val id: String,
        val name: String,
        val symbol: String,
        val tokenIcon: TokenIconState,
        val available: Boolean = true,
        val addedTokenBalanceData: TokenBalanceData? = null,
    ) : TokenToSelectState()
}

data class TokenBalanceData(
    val amount: String?,
    val amountEquivalent: String?,
    val isBalanceHidden: Boolean,
)
