package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/**
 * A chain-and-token combination the Polymarket bridge accepts for a deposit.
 *
 * @property minCheckoutUsd the minimum **deposit** for this asset in USD; it says nothing about withdrawals
 */
data class PolymarketBridgeAsset(
    val chainId: Long,
    val chainName: String,
    val tokenSymbol: String,
    val tokenAddress: String,
    val decimals: Int,
    val minCheckoutUsd: BigDecimal,
)