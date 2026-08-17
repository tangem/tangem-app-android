package com.tangem.features.polymarket.main.api.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/**
 * State of the Prediction account row on the wallet screen.
 *
 * Carries only what the account's state changes — the title and the icon are the same in every state and belong to
 * the row itself. The balance is a [TextReference] rather than a number because formatting it needs the app's
 * selected currency, which the wallet screen already holds.
 */
@Immutable
sealed class PolymarketMainUM {

    /** The row is not shown at all — the wallet has no prediction account, or the feature is off. */
    data object Hidden : PolymarketMainUM()

    /** Nothing is known yet: the row is shown with its balance shimmering, so the list does not jump later. */
    data class Loading(val subtitle: TextReference) : PolymarketMainUM()

    /**
     * @property isBalanceFlickering the balance is from the cache and a refresh is in flight
     * @property isBalanceFromCache the refresh failed and the balance shown is the last one known
     */
    data class Content(
        val subtitle: TextReference,
        val balance: TextReference,
        val isBalanceFlickering: Boolean,
        val isBalanceFromCache: Boolean,
        val onClick: () -> Unit,
    ) : PolymarketMainUM()

    /** The account's state could not be established. The row stays, so the account does not vanish and reappear. */
    data class Unavailable(val subtitle: TextReference, val onClick: () -> Unit) : PolymarketMainUM()
}