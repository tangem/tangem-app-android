package com.tangem.domain.polymarket.model

/** Failure surface of [com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner]. */
sealed interface PolymarketSigningError : PolymarketError {

    /** The owner key has not been derived yet — derive it before signing. */
    data object NotDerived : PolymarketSigningError

    /** The wallet has no secp256k1 key. */
    data object MissingWallet : PolymarketSigningError

    data object UserCancelled : PolymarketSigningError

    data object CardError : PolymarketSigningError

    data object Unknown : PolymarketSigningError
}