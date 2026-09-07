package com.tangem.domain.polymarket.model

data class PolymarketDepositTarget(
    val addresses: PolymarketBridgeAddresses,
    val asset: PolymarketBridgeAsset,
)