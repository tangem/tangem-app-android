package com.tangem.features.polymarket.impl.main.addfunds

import com.tangem.domain.models.TokenReceiveConfig

internal sealed interface PolymarketFundingSheetConfig {

    data object AddFunds : PolymarketFundingSheetConfig

    data class Receive(val config: TokenReceiveConfig) : PolymarketFundingSheetConfig
}