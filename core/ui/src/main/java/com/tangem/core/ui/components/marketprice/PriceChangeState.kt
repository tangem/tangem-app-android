package com.tangem.core.ui.components.marketprice

sealed class PriceChangeState {

    data class Content(val valueInPercent: String, val type: PriceChangeType) : PriceChangeState()

    object Unknown : PriceChangeState()
}

/** Price changing type */
enum class PriceChangeType {
    UP, DOWN
}