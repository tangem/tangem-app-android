package com.tangem.domain.polymarket.model

/**
 * Where a user lands when they open the feature. Every value is a decision already taken, so the UI renders
 * it rather than re-deriving it from the region and the wallet state.
 */
sealed interface PolymarketEntry {

    /**
     * The region allows trading and onboarding is unfinished. [status] tells the caller which action is
     * still owed, so the action button can be labelled without a second read.
     */
    data class Onboard(val status: PolymarketWalletStatus) : PolymarketEntry

    /** The region allows trading and the deposit wallet is ready. */
    data object Trade : PolymarketEntry

    /** The region forbids trading, but a deposit wallet exists — it stays viewable and withdrawable. */
    data object ReadOnly : PolymarketEntry

    /** The region forbids trading and there is no deposit wallet to fall back to. */
    data object RegionBlocked : PolymarketEntry
}