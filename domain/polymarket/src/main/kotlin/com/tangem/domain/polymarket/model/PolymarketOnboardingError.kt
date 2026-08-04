package com.tangem.domain.polymarket.model

/**
 * Failure surface of the onboarding prerequisites. Source errors are wrapped rather than flattened:
 * [Derivation] keeps user cancellation distinguishable from a real failure, and [Wallet] keeps the
 * signal that decides whether the caller may retry.
 */
sealed interface PolymarketOnboardingError : PolymarketError {

    data class Derivation(val cause: PolymarketDerivationError) : PolymarketOnboardingError

    data class Wallet(val cause: PolymarketWalletError) : PolymarketOnboardingError

    data class Signing(val cause: PolymarketSigningError) : PolymarketOnboardingError

    data class Auth(val cause: PolymarketAuthError) : PolymarketOnboardingError

    /** The backend knows a deposit wallet that is not the one derived locally. */
    data class AddressMismatch(val expected: String, val actual: String) : PolymarketOnboardingError

    /** The deploy transaction failed or timed out on-chain. */
    data object DeploymentFailed : PolymarketOnboardingError

    /** The approvals transaction failed or timed out on-chain. */
    data object ApprovalsFailed : PolymarketOnboardingError

    data object Network : PolymarketOnboardingError

    data object Unknown : PolymarketOnboardingError
}