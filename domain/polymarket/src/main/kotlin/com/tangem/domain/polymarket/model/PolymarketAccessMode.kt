package com.tangem.domain.polymarket.model

/**
 * What the user is allowed to do inside the Polymarket feature.
 *
 * [READ_ONLY] is the region-restricted mode: browsing and withdrawing stay available, trading does not.
 *
 * Its only source today is the geoblock check, and the region-restrictions copy shown to the user is worded
 * for exactly that cause. A second reason to restrict access — a suspended account, missing credentials —
 * needs its own signal rather than reusing this one, or the UI will explain the restriction wrongly.
 */
enum class PolymarketAccessMode {
    TRADING,
    READ_ONLY,
}