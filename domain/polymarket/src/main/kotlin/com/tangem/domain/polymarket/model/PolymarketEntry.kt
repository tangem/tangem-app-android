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

    /** Onboarding is complete and the feed is reachable. */
    data object Onboarded : PolymarketEntry

    /**
     * Nothing is decided yet, because deciding needs the owner address and this device has not derived it.
     * Deriving opens a card session or unlocks the wallet, and neither may happen unasked — so the Welcome
     * screen is shown and the real decision is taken when the user presses its action button.
     *
     * This is emphatically not "there is no deposit wallet": a wallet onboarded on another device resolves
     * here too, and reporting it as absent would deny that user access to their funds.
     */
    data object Undetermined : PolymarketEntry
}