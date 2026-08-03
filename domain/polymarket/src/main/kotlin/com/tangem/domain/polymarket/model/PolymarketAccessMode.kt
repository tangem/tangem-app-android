package com.tangem.domain.polymarket.model

/**
 * What the user is allowed to do inside the Polymarket feature.
 *
 * [READ_ONLY] is the region-restricted mode: browsing and withdrawing stay available, trading does not.
 */
enum class PolymarketAccessMode {
    TRADING,
    READ_ONLY,
}