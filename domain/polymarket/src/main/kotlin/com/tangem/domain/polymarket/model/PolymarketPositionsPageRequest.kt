package com.tangem.domain.polymarket.model

/**
 * The first page of a list is asked for by size, every next one by the cursor the page before it returned, which
 * also fixes the size. A cursor is opaque and bound to the list and wallet it came from: it is never built,
 * inspected or carried over to another list.
 */
sealed interface PolymarketPositionsPageRequest {

    data class First(val limit: Int) : PolymarketPositionsPageRequest

    data class Next(val cursor: String) : PolymarketPositionsPageRequest
}