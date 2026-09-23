package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable

/** Whether the order result can open the position the order created. */
@Immutable
internal sealed interface ViewPositionUM {

    /** No position to open: a sell, a market without a condition id, or a fill the exchange never indexed. */
    data object Hidden : ViewPositionUM

    /** The fill is being indexed by the exchange; the button is shown but cannot be pressed yet. */
    data object Awaiting : ViewPositionUM

    data object Available : ViewPositionUM
}