package com.tangem.features.polymarket.main.api.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/** State of the Prediction account row. The balance arrives formatted: formatting needs the app currency. */
@Immutable
sealed class PolymarketMainUM {

    data object Hidden : PolymarketMainUM()

    /** Shown with a shimmering balance, so the list does not jump once the balance arrives. */
    data class Loading(val subtitle: TextReference) : PolymarketMainUM()

    /**
     * @property isBalanceFlickering a refresh is in flight
     * @property isBalanceFromCache the refresh failed and the balance shown is the last one known
     */
    data class Content(
        val subtitle: TextReference,
        val balance: TextReference,
        val isBalanceFlickering: Boolean,
        val isBalanceFromCache: Boolean,
        val onClick: () -> Unit,
    ) : PolymarketMainUM()

    /** State unknown. The row stays, so the account does not vanish and reappear. */
    data class Unavailable(val subtitle: TextReference, val onClick: () -> Unit) : PolymarketMainUM()
}