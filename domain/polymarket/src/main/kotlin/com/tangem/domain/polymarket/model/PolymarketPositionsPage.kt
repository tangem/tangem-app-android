package com.tangem.domain.polymarket.model

/** Offset-paged holdings. An empty page is valid; the API does not supply a total count. */
data class PolymarketPositionsPage<out T : PolymarketPosition>(val positions: List<T>, val hasNext: Boolean)