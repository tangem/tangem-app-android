package com.tangem.domain.polymarket.model

/**
 * Where a deposit is sent, and what the bridge will accept there.
 *
 * @property bridgeAddress the address the user sends funds to. Never the deposit wallet itself, which does
 *  not credit a direct transfer
 * @property asset the chain-and-token combination [bridgeAddress] was issued for, carrying the minimum the
 *  bridge reported for it
 */
data class PolymarketDepositTarget(
    val bridgeAddress: String,
    val asset: PolymarketBridgeAsset,
)