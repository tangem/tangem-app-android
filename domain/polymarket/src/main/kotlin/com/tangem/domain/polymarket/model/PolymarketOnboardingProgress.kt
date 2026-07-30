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
     * Terminal: the backend is still executing [status] and this run stopped waiting. Not a failure —
     * the operation continues server-side and a later run resumes from wherever it got to.
     */
    data class StillWorking(val status: PolymarketWalletStatus) : PolymarketOnboardingProgress

    /** Terminal: [isRetryable] tells whether starting another run can plausibly succeed. */
    data class Failed(
        val error: PolymarketOnboardingError,
        val isRetryable: Boolean,
    ) : PolymarketOnboardingProgress
}