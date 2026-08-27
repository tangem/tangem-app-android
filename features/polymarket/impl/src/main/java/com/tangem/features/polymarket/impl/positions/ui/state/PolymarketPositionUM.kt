package com.tangem.features.polymarket.impl.positions.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/**
 * A single position of the user — a prediction placed on a market outcome.
 *
 * Nothing serves positions yet: the card is built ahead of the BFF, which is yet to expose them
 * (the search design already shows position results).
 *
 * @property id position id
 * @property title question of the market the position is placed on
 * @property iconUrl optional market icon
 * @property outcomeLabel label of the taken outcome, as provided by the backend
 * @property priceChange formatted price move of the position, `null` when unknown
 * @property isPriceUp whether the price moved in the position's favor, drives the [priceChange] tint
 * @property result formatted realized outcome (e.g. "Lost $1,245.00"), `null` while the position is open
 * @property isProfit whether [result] is a win, drives its tint
 * @property onClick opens the position
 */
@Immutable
internal data class PolymarketPositionUM(
    val id: String,
    val title: TextReference,
    val iconUrl: String?,
    val outcomeLabel: TextReference,
    val priceChange: TextReference?,
    val isPriceUp: Boolean,
    val result: TextReference?,
    val isProfit: Boolean,
    val onClick: () -> Unit,
)