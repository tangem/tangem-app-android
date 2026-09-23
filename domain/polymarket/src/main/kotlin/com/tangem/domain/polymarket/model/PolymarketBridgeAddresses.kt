package com.tangem.domain.polymarket.model

/**
 * @property evm serves every EVM chain the bridge accepts, not one of them
 * @property tron null when the bridge issues no Tron address for the deposit wallet
 */
data class PolymarketBridgeAddresses(
    val evm: String,
    val svm: String?,
    val btc: String?,
    val tron: String?,
)