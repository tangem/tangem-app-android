package com.tangem.data.polymarket.converter

import com.tangem.datasource.api.polymarket.models.PolymarketEventDto
import com.tangem.datasource.api.polymarket.models.PolymarketMarketDto
import com.tangem.datasource.api.polymarket.models.PolymarketOutcomeDto
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import javax.inject.Inject

internal class PolymarketEventConverter @Inject constructor() : Converter<PolymarketEventDto, PolymarketEvent> {

    override fun convert(value: PolymarketEventDto): PolymarketEvent {
        return PolymarketEvent(
            id = value.eventId,
            slug = value.slug,
            title = value.title,
            description = value.description,
            rulesUrl = value.polymarketRulesUrl,
            iconUrl = value.icon,
            imageUrl = value.image,
            status = convertStatus(status = value.status),
            startDate = value.startDate,
            endDate = value.endDate,
            volume = value.volume?.let(BigDecimal::valueOf),
            volume24h = value.volume24hr?.let(BigDecimal::valueOf),
            liquidity = value.liquidity?.let(BigDecimal::valueOf),
            totalMarketsCount = value.totalMarketsCount,
            isNegRisk = value.isNegRisk,
            displayMode = convertDisplayMode(displayMode = value.displayMode, isNegRisk = value.isNegRisk),
            markets = value.markets.map(::convertMarket),
        )
    }

    private fun convertMarket(dto: PolymarketMarketDto): PolymarketMarket {
        return PolymarketMarket(
            id = dto.id,
            eventId = dto.eventId,
            title = dto.question,
            slug = dto.slug,
            description = dto.description,
            groupItemTitle = dto.groupItemTitle,
            iconUrl = dto.icon,
            imageUrl = dto.image,
            status = convertStatus(status = dto.status),
            isNegRisk = dto.isNegRisk,
            startDate = dto.startDate,
            endDate = dto.endDate,
            startDateIso = dto.startDateIso,
            endDateIso = dto.endDateIso,
            volume = dto.volume?.let(BigDecimal::valueOf),
            volume24h = dto.volume24hr?.let(BigDecimal::valueOf),
            liquidity = dto.liquidity?.let(BigDecimal::valueOf),
            orderIndex = dto.orderIndex,
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

    private fun convertStatus(status: String): PolymarketStatus {
        return when (status) {
            STATUS_ACTIVE -> PolymarketStatus.ACTIVE
            STATUS_CLOSED -> PolymarketStatus.CLOSED
            STATUS_ARCHIVED -> PolymarketStatus.ARCHIVED
            else -> PolymarketStatus.UNKNOWN
        }
    }

    /** The BFF derives the display mode from `isNegRisk`, so the same rule backs an unrecognized [displayMode]. */
    private fun convertDisplayMode(displayMode: String, isNegRisk: Boolean): PolymarketDisplayMode {
        return when (displayMode) {
            DISPLAY_MODE_GROUPED_OUTCOMES -> PolymarketDisplayMode.GROUPED_OUTCOMES
            DISPLAY_MODE_PLAIN_MARKETS -> PolymarketDisplayMode.PLAIN_MARKETS
            else -> if (isNegRisk) PolymarketDisplayMode.GROUPED_OUTCOMES else PolymarketDisplayMode.PLAIN_MARKETS
        }
    }

    private companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_CLOSED = "closed"
        const val STATUS_ARCHIVED = "archived"

        const val DISPLAY_MODE_GROUPED_OUTCOMES = "grouped_outcomes"
        const val DISPLAY_MODE_PLAIN_MARKETS = "plain_markets"
    }
}