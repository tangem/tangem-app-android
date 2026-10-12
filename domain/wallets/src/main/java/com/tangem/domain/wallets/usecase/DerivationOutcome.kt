package com.tangem.domain.wallets.usecase

/** Outcome of a derivation that completed without errors */
enum class DerivationOutcome {

    /** At least one of the requested keys was derived */
    DERIVED,

    /** Nothing was missing, so the card was never asked to derive */
    NOTHING_MISSING,

    /**
     * The wallet answered for every requested seed key and derived not a single path. A missing wallet is a soft
     * error for the SDK derivation task — it keeps deriving the remaining paths to support partial derivation — so
     * a card that holds none of the requested wallets, such as a factory-reset one, completes the session this way.
     */
    NOTHING_DERIVED,
}