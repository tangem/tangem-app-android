package com.tangem.grow.datasource.polymarket.clob

import java.math.BigDecimal

/** How the CLOB answers `GET /price`; shared by every flow that reads a book price. */
object PolymarketClobPriceFormat {

    /** What an empty bid answers instead of a price — `side=BUY` reads the bid. */
    val EMPTY_BID_PRICE: BigDecimal = BigDecimal.ZERO

    /** What an empty ask answers instead of a price — `side=SELL` reads the ask. */
    val EMPTY_ASK_PRICE: BigDecimal = BigDecimal.ONE
}