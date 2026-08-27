package com.tangem.domain.polymarket.model

/**
 * Typed failure surface of Polymarket owner-EOA derivation.
 */
sealed interface PolymarketDerivationError : PolymarketError {

    /** No secp256k1 key material on the wallet (card has no such wallet / hot wallet locked). */
    data object MissingWallet : PolymarketDerivationError

    /** The user cancelled the NFC card session. */
    data object UserCancelled : PolymarketDerivationError

    /** Firmware without HD derivation support — this wallet cannot onboard to Polymarket. */
    data object DerivationUnsupported : PolymarketDerivationError

    /** Any other card-session failure. */
    data object CardError : PolymarketDerivationError

    /** Any non-card, unexpected failure. */
    data object Unknown : PolymarketDerivationError
}