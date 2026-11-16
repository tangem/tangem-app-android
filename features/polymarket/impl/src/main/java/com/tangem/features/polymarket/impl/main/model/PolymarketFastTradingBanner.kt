package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest

internal sealed interface PolymarketFastTradingBanner {

    data object UnknownSigners : PolymarketFastTradingBanner

    data object Activate : PolymarketFastTradingBanner

    data class Reactivate(val request: PolymarketSessionKeyOperationRequest) : PolymarketFastTradingBanner

    data class Extend(val daysLeft: Int) : PolymarketFastTradingBanner
}