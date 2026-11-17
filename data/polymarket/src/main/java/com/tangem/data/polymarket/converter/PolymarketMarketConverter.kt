package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.grow.datasource.polymarket.models.PolymarketMarketDto
import com.tangem.grow.datasource.polymarket.models.PolymarketOutcomeDto
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal object PolymarketMarketConverter : Converter<PolymarketMarketDto, PolymarketMarket> {
    override fun convert(dto: PolymarketMarketDto): PolymarketMarket {
        return PolymarketMarket(
            id = dto.id,
            conditionId = dto.conditionId,
            eventId = dto.eventId,
            title = dto.question,
            slug = dto.slug,
            description = dto.description,
            groupItemTitle = dto.groupItemTitle,
            iconUrl = dto.icon,
            imageUrl = dto.image,
            status = PolymarketStatusConverter.convert(dto.status),
            isNegRisk = dto.isNegRisk,
            isAcceptingOrders = dto.isAcceptingOrders == true,
            startDate = dto.startDate,
            endDate = dto.endDate,
            startDateIso = dto.startDateIso,
            endDateIso = dto.endDateIso,
            volume = dto.volume?.let(BigDecimal::valueOf),
            volume24h = dto.volume24hr?.let(BigDecimal::valueOf),
            liquidity = dto.liquidity?.let(BigDecimal::valueOf),
            orderIndex = dto.orderIndex,
            outcomes = dto.outcomes.map(::convertOutcome),
            version = PolymarketMarketVersionConverter.convert(dto.version),
            sportsMarketType = dto.sportsMarketType,
            line = dto.line?.let(BigDecimal::valueOf),
        )
    }

    private fun convertOutcome(dto: PolymarketOutcomeDto): PolymarketOutcome {
        return PolymarketOutcome(
            assetId = dto.assetId,
            title = dto.label,
            probability = dto.probability?.let(BigDecimal::valueOf),
            legacyAssetId = dto.legacyAssetId,
        )
    }
}