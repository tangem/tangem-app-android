package com.tangem.domain.tokens.model

data class NetworkGroup(
    val networkId: Network.ID,
    val name: String,
    val tokens: Set<TokenStatus>,
)
