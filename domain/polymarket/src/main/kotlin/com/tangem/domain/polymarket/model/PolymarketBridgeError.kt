package com.tangem.domain.polymarket.model

/** Failures of the deposit and withdrawal machinery Polymarket runs at `bridge.polymarket.com`. */
sealed interface PolymarketBridgeError : PolymarketError {

    /** The bridge could not be reached. */
    data object Network : PolymarketBridgeError

    /**
     * The bridge answered, but this device cannot offer a deposit from what it said: the asked-for asset is
     * absent from the accepted list, or the wallet's deposit wallet is unknown here.
     */
    data object Unavailable : PolymarketBridgeError
}