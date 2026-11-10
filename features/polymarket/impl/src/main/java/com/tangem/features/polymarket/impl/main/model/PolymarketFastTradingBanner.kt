package com.tangem.features.polymarket.impl.main.model

internal sealed interface PolymarketFastTradingBanner {

    data object UnknownSigners : PolymarketFastTradingBanner

    data object Activate : PolymarketFastTradingBanner
}