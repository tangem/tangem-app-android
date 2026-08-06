package com.tangem.domain.polymarket.model

/** Progress of an onboarding run: every value is a decision already taken, not a status to interpret. */
sealed interface PolymarketOnboardingProgress {

    /** Deriving the owner address; may require a card session. */
    data object Deriving : PolymarketOnboardingProgress

    /** Both onboarding payloads are being signed in one card session. */
    data object AwaitingSignature : PolymarketOnboardingProgress

    /** The backend is executing [status]. */
    data class Working(val status: PolymarketWalletStatus) : PolymarketOnboardingProgress

    /** Terminal: the wallet is onboarded and the credentials are stored. */
    data object Ready : PolymarketOnboardingProgress

    /**
     * Terminal: [isRetryable] tells whether starting another run can plausibly succeed.
     *
     * The UI does not currently read [isRetryable] — both values return the Start button to idle.
     * The flag is reserved for the error design that is still to be agreed.
     */
    data class Failed(
        val error: PolymarketOnboardingError,
        val isRetryable: Boolean,
    ) : PolymarketOnboardingProgress
}