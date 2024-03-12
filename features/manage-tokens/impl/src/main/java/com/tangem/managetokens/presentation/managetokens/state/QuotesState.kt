package com.tangem.managetokens.presentation.managetokens.state

import com.tangem.core.ui.components.marketprice.PriceChangeType
import kotlinx.collections.immutable.ImmutableList

internal sealed class QuotesState {
    object Unknown : QuotesState()

    data class Content(
        val priceChange: String,
        val changeType: PriceChangeType,
        val chartData: ImmutableList<Float>,
    ) : QuotesState()
}