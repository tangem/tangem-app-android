package com.tangem.features.polymarket.impl.positions.model

import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import java.math.BigDecimal
import java.time.LocalDate

internal fun createPolymarketPosition(
    assetId: String = "asset",
    outcome: String = "Yes",
    cashPnl: BigDecimal = BigDecimal("-0.0280"),
    percentPnl: BigDecimal = BigDecimal("-0.2800"),
    realizedPnl: BigDecimal = BigDecimal("12"),
    result: PolymarketPositionResult? = null,
    resolutionStatus: PolymarketResolutionStatus = PolymarketResolutionStatus.OPEN,
): PolymarketPosition.Open = PolymarketPosition.Open(
    id = PolymarketPositionId(conditionId = "condition", assetId = assetId),
    title = "Will the event happen?",
    iconUrl = null,
    outcome = outcome,
    endDate = LocalDate.of(2027, 4, 30),
    avgPrice = BigDecimal("0.3559"),
    curPrice = BigDecimal("0.355"),
    realizedPnl = realizedPnl,
    marketId = "market",
    eventId = "event",
    eventTitle = "Event",
    size = BigDecimal("28.0898"),
    initialValue = BigDecimal("9.9999"),
    currentValue = BigDecimal("9.9719"),
    cashPnl = cashPnl,
    percentPnl = percentPnl,
    totalBought = BigDecimal("28.0898"),
    isRedeemable = false,
    isNegRisk = true,
    isAcceptingOrders = true,
    isLive = false,
    resolutionStatus = resolutionStatus,
    result = result,
)