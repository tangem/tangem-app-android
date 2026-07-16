package com.tangem.data.polymarket.converter

import com.tangem.datasource.api.polymarket.models.PolymarketEventDto
import com.tangem.datasource.api.polymarket.models.PolymarketMarketDto
import com.tangem.datasource.api.polymarket.models.PolymarketOutcomeDto
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import javax.inject.Inject

internal class PolymarketEventConverter @Inject constructor() : Converter<PolymarketEventDto, PolymarketEvent> {

    override fun convert(value: PolymarketEventDto): PolymarketEvent {
        return PolymarketEvent(
            id = value.eventId,
            slug = value.slug,
            title = value.title,
            iconUrl = value.icon,
            imageUrl = value.image,
            volume = value.volume?.let(BigDecimal::valueOf),
            totalMarketsCount = value.totalMarketsCount,
            markets = value.markets.map(::convertMarket),
        )
    }

    private fun convertMarket(dto: PolymarketMarketDto): PolymarketMarket {
        return PolymarketMarket(
            id = dto.id,
            title = dto.question,
            iconUrl = dto.icon,
            volume = dto.volume?.let(BigDecimal::valueOf),
            outcomes = dto.outcomes.map(::convertOutcome),
        )
    }

    private fun convertOutcome(dto: PolymarketOutcomeDto): PolymarketOutcome {
        return PolymarketOutcome(
            assetId = dto.assetId,
            title = dto.label,
            probability = dto.probability?.let(BigDecimal::valueOf),
        )
    }
}