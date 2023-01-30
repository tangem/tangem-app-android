package com.tangem.feature.swap.models

data class SwapSelectTokenStateHolder(
    val addedTokens: List<TokenToSelect>,
    val otherTokens: List<TokenToSelect>,
    val network: Network,
    val onSearchEntered: (String) -> Unit,
    val onTokenSelected: (String) -> Unit,
)

data class TokenToSelect(
    val id: String,
    val name: String,
    val symbol: String,
    val iconUrl: String,
    val isNative: Boolean,
    val available: Boolean = true,
    val addedTokenBalanceData: TokenBalanceData? = null,
)

data class Network(
    val name: String,
    val blockchainId: String,
)

data class TokenBalanceData(
    val amount: String?,
    val amountEquivalent: String?,
)
