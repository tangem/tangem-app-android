package com.tangem.grow.datasource.polymarket.socket

/** Close codes the live-data socket defines itself, on top of the ones of the WebSocket protocol. */
internal object PolymarketPriceCloseCode {
    const val AUTHENTICATION_FAILED = 4001
    const val SERVER_DRAINING = 4003
    const val POLICY_VIOLATION = 4008
}