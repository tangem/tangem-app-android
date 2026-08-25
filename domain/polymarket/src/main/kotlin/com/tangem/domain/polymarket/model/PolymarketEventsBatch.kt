package com.tangem.domain.polymarket.model

/**
 * @property requestCursor the cursor this page was *requested* with, not the one its response carried — that one
 *  leads to the next page. Refreshing a page means asking for it again with this cursor.
 */
data class PolymarketEventsBatch(
    val events: List<PolymarketEvent>,
    val requestCursor: String?,
)