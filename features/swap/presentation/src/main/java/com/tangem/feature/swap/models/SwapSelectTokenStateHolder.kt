package com.tangem.feature.swap.models

data class SwapSelectTokenStateHolder(
    val tokens: List<TokenToSelect>,
    val onSearchEntered: (String) -> Unit,
    val onTokenSelected: (String) -> Unit,
)

data class TokenToSelect(
    val id: String,
    val name: String,
    val symbol: String,
    val iconUrl: String,
    val available: Boolean = true,
    val addedTokenBalanceData: TokenBalanceData? = null,
)

data class TokenBalanceData(
    val amount: String?,
    val amountEquivalent: String?,
)
