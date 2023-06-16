package com.tangem.domain.tokens.model

import arrow.core.NonEmptySet

data class NetworkGroup(
    val network: Network,
    val tokens: NonEmptySet<TokenState>,
)
