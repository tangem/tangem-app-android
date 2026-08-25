package com.tangem.domain.polymarket.model

/**
 * A loaded page of the Discovery feed, as the pagination holds it.
 *
 * @property events events of the page, in the order the backend served them
 * @property requestCursor the cursor this page was *requested* with — `null` for the first one. Not the cursor its
 *  response carried: that one leads to the page after this. Refreshing a page means asking for it again with
 *  [requestCursor], so it stays with the page for as long as the page is on the screen.
 */
data class PolymarketEventsBatch(
    val events: List<PolymarketEvent>,
    val requestCursor: String?,
)