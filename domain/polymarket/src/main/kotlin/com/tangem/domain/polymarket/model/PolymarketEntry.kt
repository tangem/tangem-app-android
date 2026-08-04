package com.tangem.domain.polymarket.model

/**
 * Where a user lands when they open the feature. Every value is a decision already taken, so the UI renders
 * it rather than re-deriving it from the region and the wallet state.
 */
sealed interface PolymarketEntry {

    /**
     * The region allows trading and onboarding is unfinished. [status] tells the caller which action is
     * still owed, so the action button can be labelled without a second read. The onboarding model reads it
     * to pick the Welcome screen's start-button label — a fresh "Start" versus a resuming "Continue".
     */
    data class Onboard(val status: PolymarketWalletStatus) : PolymarketEntry

    /**
     * Onboarding is complete and the feed is reachable. [accessMode] carries whether trading is permitted:
     * a region that forbids trading still leaves an existing deposit wallet viewable and withdrawable, so
     * it resolves here rather than being turned away.
     */
    data class Onboarded(val accessMode: PolymarketAccessMode) : PolymarketEntry

    /** The region forbids trading and there is no deposit wallet to fall back to. */
    data object RegionBlocked : PolymarketEntry
}