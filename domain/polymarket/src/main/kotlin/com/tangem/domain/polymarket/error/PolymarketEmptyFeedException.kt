package com.tangem.domain.polymarket.error

/**
 * The Discovery feed came back empty where content was expected.
 *
 * A category with no events is presented to the user exactly like a failed load (per design), so the pagination
 * reports it as a failure rather than as an empty-but-successful page.
 */
class PolymarketEmptyFeedException : Exception("Polymarket feed returned no events")