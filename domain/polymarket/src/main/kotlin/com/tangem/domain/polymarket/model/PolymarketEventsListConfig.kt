package com.tangem.domain.polymarket.model

/**
 * Request params of the Discovery feed pagination.
 *
 * @property category category id to filter by; `null` for the unfiltered feed the backend serves by default
 */
data class PolymarketEventsListConfig(
    val category: Int? = null,
)