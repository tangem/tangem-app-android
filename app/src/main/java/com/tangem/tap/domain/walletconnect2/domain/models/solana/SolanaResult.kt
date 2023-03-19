package com.tangem.tap.domain.walletconnect2.domain.models.solana

import kotlinx.serialization.Serializable

@Serializable
data class SolanaResult(
    val signature: String,
)