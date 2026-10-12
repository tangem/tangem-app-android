package com.tangem.domain.polymarket.model

/**
 * Refreshes a single loaded page of the Discovery feed in place.
 *
 * @property batchKey comes back with the update result, which is how the caller tells which page succeeded
 */
data class PolymarketEventsUpdateRequest(
    val batchKey: Int,
    val config: PolymarketEventsListConfig,
)