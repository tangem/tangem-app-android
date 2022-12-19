package com.tangem.feature.swap.models

data class SwapSelectTokenStateHolder(
    val tokens: List<TokenToSelect>,
    val onSearchEntered: (String) -> Unit,
    val onTokenSelected: (String) -> Unit,
)