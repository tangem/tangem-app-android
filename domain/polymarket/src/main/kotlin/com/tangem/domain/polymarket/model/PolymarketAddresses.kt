package com.tangem.domain.polymarket.model

/**
 * The owner EOA and the deposit wallet derived from it. The two always travel together so that every
 * consumer signs, deploys and verifies against the same pair.
 */
data class PolymarketAddresses(
    val ownerAddress: String,
    val depositWalletAddress: String,
)