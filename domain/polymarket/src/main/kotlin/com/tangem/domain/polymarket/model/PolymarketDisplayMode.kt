package com.tangem.domain.polymarket.model

/**
 * Defines how an event and its nested markets are rendered (BFF `displayMode`).
 */
enum class PolymarketDisplayMode {

    /**
     * The event's markets are mutually exclusive, so the card lists markets as a single set of competing outcomes.
     * Set by the BFF when [PolymarketEvent.isNegRisk] is `true`.
     */
    GROUPED_OUTCOMES,

    /** The event's markets are independent, so the card lists every market with its own outcomes */
    PLAIN_MARKETS,
}