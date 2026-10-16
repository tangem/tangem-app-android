package com.tangem.domain.polymarket.model

/** One page of a cursor-paged list. [nextCursor] continues it and `null` ends it; an empty page is valid. */
data class PolymarketPositionsPage<out T : PolymarketPosition>(val positions: List<T>, val nextCursor: String?)