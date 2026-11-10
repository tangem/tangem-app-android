package com.tangem.grow.datasource.polymarket.clob

/** How the CLOB describes an order in `GET /data/order/{id}`; shared by every flow that reads an order back. */
object PolymarketClobOrderFormat {

    /** The order has been matched in full or in part; the fill is `size_matched`. */
    const val STATUS_MATCHED: String = "ORDER_STATUS_MATCHED"

    val REJECTED_STATUSES: Set<String> = setOf(
        "ORDER_STATUS_CANCELED",
        "ORDER_STATUS_CANCELED_MARKET_RESOLVED",
        "ORDER_STATUS_INVALID",
    )

    /** `original_size` and `size_matched` come in share base units with this many decimals. */
    const val SHARE_DECIMALS: Int = 6
}