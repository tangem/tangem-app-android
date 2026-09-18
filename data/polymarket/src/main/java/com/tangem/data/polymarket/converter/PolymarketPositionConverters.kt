package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketClosedPositionStatus
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.grow.datasource.polymarket.models.PolymarketClosedPositionDto
import com.tangem.grow.datasource.polymarket.models.PolymarketOpenPositionDto
import com.tangem.utils.converter.Converter
import java.time.Instant
import java.time.LocalDate

internal object PolymarketOpenPositionConverter : Converter<PolymarketOpenPositionDto, PolymarketPosition.Open> {

    override fun convert(value: PolymarketOpenPositionDto): PolymarketPosition.Open = PolymarketPosition.Open(
        id = PolymarketPositionId(conditionId = value.conditionId, assetId = value.assetId),
        title = value.title,
        iconUrl = value.icon,
        outcome = value.outcome,
        outcomeIndex = value.outcomeIndex,
        endDate = value.endDate.toDateOrNull(),
        avgPrice = value.avgPrice.toBigDecimal(),
        curPrice = value.curPrice.toBigDecimal(),
        realizedPnl = value.realizedPnl.toBigDecimal(),
        marketId = value.marketId,
        eventId = value.eventId,
        eventTitle = value.eventTitle,
        size = value.size.toBigDecimal(),
        initialValue = value.initialValue.toBigDecimal(),
        currentValue = value.currentValue.toBigDecimal(),
        cashPnl = value.cashPnl.toBigDecimal(),
        percentPnl = value.percentPnl.toBigDecimal(),
        totalBought = value.totalBought.toBigDecimal(),
        isRedeemable = value.isRedeemable,
        isNegRisk = value.isNegRisk,
        isAcceptingOrders = value.isAcceptingOrders,
        isLive = value.isLive,
        resolutionStatus = PolymarketResolutionStatus.entries.firstOrNull { it.name == value.resolutionStatus }
            ?: PolymarketResolutionStatus.UNKNOWN,
        result = value.status?.let { status ->
            PolymarketPositionResult.entries.firstOrNull { it.name == status } ?: PolymarketPositionResult.UNKNOWN
        },
    )
}

internal object PolymarketClosedPositionConverter : Converter<PolymarketClosedPositionDto, PolymarketPosition.Closed> {

    override fun convert(value: PolymarketClosedPositionDto): PolymarketPosition.Closed = PolymarketPosition.Closed(
        id = PolymarketPositionId(conditionId = value.conditionId, assetId = value.assetId),
        title = value.title,
        iconUrl = value.icon,
        outcome = value.outcome,
        outcomeIndex = value.outcomeIndex,
        endDate = value.endDate.toDateOrNull(),
        avgPrice = value.avgPrice.toBigDecimal(),
        curPrice = value.curPrice.toBigDecimal(),
        realizedPnl = value.realizedPnl.toBigDecimal(),
        totalBought = value.totalBought.toBigDecimal(),
        closedAt = Instant.parse(value.closedAt),
        status = PolymarketClosedPositionStatus.entries.firstOrNull { it.name == value.status }
            ?: PolymarketClosedPositionStatus.UNKNOWN,
    )
}

private fun String?.toDateOrNull(): LocalDate? = this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }