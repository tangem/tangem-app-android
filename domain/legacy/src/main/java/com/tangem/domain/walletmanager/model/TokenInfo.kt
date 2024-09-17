package com.tangem.domain.walletmanager.model

import com.tangem.domain.tokens.model.Network

data class TokenInfo(
    val network: Network,
    val name: String,
    val symbol: String,
    val contractAddress: String,
    val decimals: Int,
    val id: String? = null,
)
