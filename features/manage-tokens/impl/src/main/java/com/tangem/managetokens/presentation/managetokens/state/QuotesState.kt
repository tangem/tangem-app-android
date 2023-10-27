package com.tangem.managetokens.presentation.managetokens.state

import kotlinx.collections.immutable.ImmutableList

internal sealed class QuotesState {
    object Unknown : QuotesState()

    data class Content(
        val priceChange: String,
        val changeType: PriceChangeType,
        val chartData: ImmutableList<Float>,
    ) : QuotesState()
}

enum class PriceChangeType {
    UP, DOWN
}