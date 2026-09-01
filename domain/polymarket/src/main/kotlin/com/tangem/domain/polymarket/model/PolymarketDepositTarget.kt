package com.tangem.domain.polymarket.model

data class PolymarketDepositTarget(
    val bridgeAddress: String,
    val asset: PolymarketBridgeAsset,
)