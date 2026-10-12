package com.tangem.features.polymarket.impl.positionresult.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM

@Immutable
internal data class PolymarketPositionResultUM(
    val market: MarketHeaderUM,
    val result: Result,
    val title: TextReference,
    val resolvedAt: TextReference?,
    val invested: Metric?,
    val profit: Metric?,
    val footerNote: TextReference?,
    val action: Action,
) {

    enum class Result { Won, Lost, Split }

    @Immutable
    sealed interface Action {

        data class Claim(val isInProgress: Boolean, val label: TextReference) : Action

        data object Dismiss : Action
    }

    @Immutable
    data class Metric(val title: TextReference, val value: TextReference)
}