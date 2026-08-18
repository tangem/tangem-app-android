package com.tangem.domain.polymarket.model

/**
 * A single page of the Discovery feed (BFF `EventsPageResponse`).
 *
 * @property events events of this page
 * @property cursor keyset cursor to pass to fetch the page after this one; `null` when the BFF reports none
 * @property hasNext whether the BFF has more pages after this one
 */
data class PolymarketEventsPage(
    val events: List<PolymarketEvent>,
    val cursor: String?,
    val hasNext: Boolean,
)