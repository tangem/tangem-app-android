package com.tangem.domain.polymarket.model

/** Both parts are required: a market can contain more than one outcome held by the same wallet. */
data class PolymarketPositionId(val conditionId: String, val assetId: String)