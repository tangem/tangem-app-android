package com.tangem.feature.swap.models

import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import kotlinx.collections.immutable.ImmutableList

data class SwapSelectTokenStateHolder(
    val availableTokens: ImmutableList<TokenToSelectState>,
    val unavailableTokens: ImmutableList<TokenToSelectState>,
    val network: Network,
    val onSearchEntered: (String) -> Unit,
    val onTokenSelected: (String) -> Unit,
)

data class Network(
    val name: String,
    val blockchainId: String,
)

sealed class TokenToSelectState {

    data class Title(val title: String) : TokenToSelectState()

    data class TokenToSelect(
        val id: String,
        val name: String,
        val symbol: String,
        val isNative: Boolean,
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