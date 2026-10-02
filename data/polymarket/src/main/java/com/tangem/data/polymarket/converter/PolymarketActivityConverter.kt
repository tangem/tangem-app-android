package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.grow.datasource.polymarket.models.PolymarketActivityDto
import com.tangem.grow.datasource.polymarket.models.PolymarketActivityEventDto
import com.tangem.grow.datasource.polymarket.models.PolymarketActivityMarketDto
import com.tangem.grow.datasource.polymarket.models.PolymarketActivityOrderDto
import com.tangem.utils.converter.Converter
import java.time.Instant

internal object PolymarketActivityConverter : Converter<PolymarketActivityDto, PolymarketActivity> {

    override fun convert(value: PolymarketActivityDto): PolymarketActivity = PolymarketActivity(
        id = value.id,
        type = PolymarketActivity.Type.valueOf(value.type),
        occurredAt = Instant.parse(value.occurredAt),
        amount = value.amount.toBigDecimal(),
        transactionHash = value.transactionHash,
        market = value.market?.toDomain(),
        order = value.order?.toDomain(),
        redeemResult = value.redeem?.let { PolymarketActivity.RedeemResult.valueOf(it.result) },
    )

    private fun PolymarketActivityMarketDto.toDomain() = PolymarketActivity.Market(
        conditionId = conditionId,
        outcomeIndex = outcomeIndex,
        shares = shares.toBigDecimal(),
        outcome = outcome,
        marketId = marketId,
        slug = slug,
        title = title,
        icon = icon,
        image = image,
        endDate = endDate?.let(Instant::parse),
        isNegRisk = isNegRisk,
        event = event?.toDomain(),
    )

    private fun PolymarketActivityOrderDto.toDomain() = PolymarketActivity.Order(
        assetId = assetId,
        averagePrice = averagePrice.toBigDecimal(),
        platformFee = platformFee.toBigDecimal(),
        builderFee = builderFee.toBigDecimal(),
        notional = notional.toBigDecimal(),
        tradeCount = tradeCount,
    )

    private fun PolymarketActivityEventDto.toDomain() = PolymarketActivity.Event(
        eventId = eventId,
        slug = slug,
        title = title,
        icon = icon,
        image = image,
        endDate = endDate?.let(Instant::parse),
    )
}