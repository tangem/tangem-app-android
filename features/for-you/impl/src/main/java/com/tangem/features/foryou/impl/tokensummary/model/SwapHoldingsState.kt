package com.tangem.features.foryou.impl.tokensummary.model

import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry

/** Outcome of resolving the user's holdings of the summary token across every wallet. */
internal sealed interface SwapHoldingsState {

    /** Balances are still loading, or the swap availability of a holding is still being resolved. */
    data object Loading : SwapHoldingsState

    /** The token is held, but every holding has a zero balance. */
    data object ZeroBalance : SwapHoldingsState

    /** [entries] are the holdings a swap can actually be started from. Never empty. */
    data class Available(val entries: List<TokenSelectorEntry>) : SwapHoldingsState

    /** No holding can be swapped from — either none has a swappable status, or the token is not held at all. */
    data object Unavailable : SwapHoldingsState
}