package com.tangem.features.foryou.impl.tokensummary.model

/** Outcome of resolving the user's holdings of the summary token across every wallet. */
internal sealed interface SwapHoldingsState {

    /** Balances are still loading, or the swap availability of a holding is still being resolved. */
    data object Loading : SwapHoldingsState

    /** The token is held nowhere, so the only thing left to offer is adding it to a portfolio. */
    data object NotHeld : SwapHoldingsState

    /** The token is held, but every holding has a zero balance. */
    data object ZeroBalance : SwapHoldingsState

    /**
     * [holdings] are every holding with funds, each carrying its own swap availability — the summary offers Swap and
     * resolves what a holding can actually do only once it is picked. Never empty.
     */
    data class Resolved(val holdings: List<SwapHolding>) : SwapHoldingsState

    /** The token has no market identity — a custom token — so neither swapping nor adding it can be resolved. */
    data object Unavailable : SwapHoldingsState
}