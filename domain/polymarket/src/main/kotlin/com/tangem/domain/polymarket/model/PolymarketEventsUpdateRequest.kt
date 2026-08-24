package com.tangem.domain.polymarket.model

/**
 * Request to refresh a single loaded page of the Discovery feed in place.
 *
 * @property batchKey key of the page to refresh; it comes back with the update result, which is how the caller
 *  tells which page succeeded or failed
 * @property config request params of the feed the page belongs to — a refresh repeats the original request,
 *  which filters by category
 */
data class PolymarketEventsUpdateRequest(
    val batchKey: Int,
    val config: PolymarketEventsListConfig,
)