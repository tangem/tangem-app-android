package com.tangem.domain.tokens.model

import arrow.core.NonEmptySet

data class NetworkGroup(
    val networkId: Network.ID,
    val name: String,
    val tokens: NonEmptySet<TokenStatus>,
)
