package com.tangem.data.polymarket.quote

import java.math.BigDecimal

internal data class PolymarketQuoteUpdate(
    val assetId: String,
    val timestamp: Long,
    val bid: Side? = null,
    val ask: Side? = null,
    val lastTradePrice: BigDecimal? = null,
) {

    /** An absent Side leaves the book unchanged; a present Side(null) clears it. */
    data class Side(val price: BigDecimal?)
}