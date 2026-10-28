package com.tangem.features.polymarket.impl.history.model

import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.domain.polymarket.model.PolymarketActivity.Type
import java.math.BigDecimal

internal fun historyTrade(type: Type = Type.BUY) = createHistoryActivity(type = type).copy(
    market = PolymarketActivity.Market(
        conditionId = "condition",
        outcomeIndex = 1,
        shares = BigDecimal("6.172836"),
        outcome = "Aurora Gaming",
        marketId = "market",
        slug = "market-slug",
        title = "Counter-Strike: 1WIN vs Aurora Gaming",
        icon = null,
        image = null,
        endDate = null,
        isNegRisk = false,
        event = PolymarketActivity.Event("event", "event-slug", "Counter-Strike", null, null, null),
    ),
    order = PolymarketActivity.Order(
        assetId = "asset",
        averagePrice = BigDecimal("0.810000"),
        platformFee = BigDecimal("0.047490"),
        builderFee = BigDecimal("0.024990"),
        notional = BigDecimal("4.999997"),
        tradeCount = 1,
    ),
)