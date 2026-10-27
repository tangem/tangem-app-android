package com.tangem.features.polymarket.api

interface PolymarketFeatureToggles {
    val isPolymarketEnabled: Boolean
    val isPolymarketPhaseTwoEnabled: Boolean
    val isPolymarketSessionKeysEnabled: Boolean
}