package com.tangem.features.polymarket.api

interface PolymarketFeatureToggles {
    val isPolymarketPhaseTwoEnabled: Boolean
    val isPolymarketFastOrdersEnabled: Boolean

    /** Crypto Up/Down events. On only together with phase two and fast orders, which the screen builds on. */
    val isPolymarketCryptoUpDownEnabled: Boolean
}