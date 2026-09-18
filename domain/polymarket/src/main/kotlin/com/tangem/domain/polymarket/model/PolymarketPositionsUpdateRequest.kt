package com.tangem.domain.polymarket.model

/** Re-reads loaded pages of a positions list in place; the pages themselves are named by the action's keys. */
data class PolymarketPositionsUpdateRequest(val config: PolymarketPositionsListConfig)