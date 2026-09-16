package com.tangem.features.polymarket.impl.positionresult.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/** UI-only state for a resolved position. Claim and dismiss actions will be added with the settlement flow. */
@Immutable
internal data class PolymarketPositionResultUM(
    val result: Result,
    val title: TextReference,
    val resolvedAt: TextReference,
    val invested: Metric?,
    val profit: Metric?,
    val footerNote: TextReference?,
) {

    enum class Result { Won, Lost, Split }

    @Immutable
    data class Metric(val title: TextReference, val value: TextReference)
}