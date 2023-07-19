package com.tangem.domain.tokens.model

data class NetworkGroup(
    val network: Network,
    val tokens: Set<TokenStatus>,
)