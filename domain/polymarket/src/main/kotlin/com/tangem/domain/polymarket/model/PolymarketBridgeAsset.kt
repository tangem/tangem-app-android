package com.tangem.domain.polymarket.model

import java.math.BigDecimal

data class PolymarketBridgeAsset(
    val chainId: String,
    val chainName: String,
    val tokenSymbol: String,
    val tokenAddress: String,
    val decimals: Int,
    val minCheckoutUsd: BigDecimal?,
)