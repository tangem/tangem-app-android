package com.tangem.domain.polymarket.model

/**
 * @property evm serves every EVM chain the bridge accepts, not one of them
 * @property tron null when the payload names the field `tvm`, which Polymarket's own pages do inconsistently
 */
data class PolymarketBridgeAddresses(
    val evm: String,
    val svm: String?,
    val btc: String?,
    val tron: String?,
)