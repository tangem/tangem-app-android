package com.tangem.features.tangempay.cashback.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * State for the monthly earnings histogram on the Cashback screen.
 *
 * @property title formatted total, e.g. "$132.15 earned in total"
 * @property bars one bar per calendar month, ordered oldest to newest
 */
@Immutable
internal data class TangemPayCashbackHistogramUM(
    val title: TextReference,
    val bars: ImmutableList<Bar>,
) {

    /**
     * @property amount formatted earnings for this month, e.g. "$12.02" (or "-$2.15" for a refund)
     * @property amountValue numeric earnings, used only to size the bar relative to the others
     * @property style visual treatment; only the current (last) month is highlighted
     */
    @Immutable
    data class Bar(
        val month: TextReference,
        val amount: TextReference,
        val amountValue: Float,
        val style: Style,
    )

    enum class Style { Regular, Highlighted, HighlightedNegative }
}