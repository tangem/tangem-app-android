package com.tangem.domain.polymarket.model

/**
 * Request params of the events search batch flow.
 *
 * @property query full-text query the pages are served for; a reload with a new config restarts the
 *  search from its first page
 */
data class PolymarketSearchConfig(
    val query: String,
)