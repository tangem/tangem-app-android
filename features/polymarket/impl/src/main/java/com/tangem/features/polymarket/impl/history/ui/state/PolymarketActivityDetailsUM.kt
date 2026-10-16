package com.tangem.features.polymarket.impl.history.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PolymarketActivityDetailsUM(
    val title: TextReference,
    val date: TextReference,
    val headline: TextReference?,
    val amount: TextReference?,
    val marketTitle: TextReference?,
    val iconUrl: String?,
    val outcome: TextReference?,
    val outcomeStatus: TangemBadge.Status,
    val outcomePrice: TextReference? = null,
    val rows: ImmutableList<Row>,
    val onGoToPrediction: (() -> Unit)?,
) {
    @Immutable
    data class Row(val title: TextReference, val value: TextReference)
}