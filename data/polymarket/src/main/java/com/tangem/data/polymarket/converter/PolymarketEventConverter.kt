package com.tangem.data.polymarket.converter

import com.tangem.grow.datasource.polymarket.models.PolymarketEventDto
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.time.Instant

internal object PolymarketEventConverter : Converter<PolymarketEventDto, PolymarketEvent> {

    private const val DISPLAY_MODE_GROUPED_OUTCOMES = "grouped_outcomes"
    private const val DISPLAY_MODE_PLAIN_MARKETS = "plain_markets"

    override fun convert(value: PolymarketEventDto): PolymarketEvent {
        return PolymarketEvent(
            id = value.eventId,
            eventType = value.eventType,
            slug = value.slug,
            title = value.title,
            description = value.description,
            rulesUrl = value.polymarketRulesUrl,
            resolutionSource = value.resolutionSource,
            iconUrl = value.icon,
            imageUrl = value.image,
            status = PolymarketStatusConverter.convert(value.status),
            startDate = value.startDate,
            endDate = value.endDate,
            volume = value.volume?.let(BigDecimal::valueOf),
            volume24h = value.volume24hr?.let(BigDecimal::valueOf),
            liquidity = value.liquidity?.let(BigDecimal::valueOf),
            totalMarketsCount = value.totalMarketsCount,
            isNegRisk = value.isNegRisk,
            isLive = value.isLive,
            displayMode = convertDisplayMode(displayMode = value.displayMode, isNegRisk = value.isNegRisk),
            markets = value.markets.map(PolymarketMarketConverter::convert),
            gameId = value.gameId,
            score = value.score,
            period = value.period,
            isEnded = value.isEnded,
            dataFetchedAt = value.dataFetchedAt?.let(Instant::parse),
            teams = value.teams?.map(PolymarketTeamConverter::convert),
        )
    }

    /** The BFF derives the display mode from `isNegRisk`, so the same rule backs an unrecognized [displayMode]. */
    private fun convertDisplayMode(displayMode: String, isNegRisk: Boolean): PolymarketDisplayMode {
        return when (displayMode) {
            DISPLAY_MODE_GROUPED_OUTCOMES -> PolymarketDisplayMode.GROUPED_OUTCOMES
            DISPLAY_MODE_PLAIN_MARKETS -> PolymarketDisplayMode.PLAIN_MARKETS
            else -> if (isNegRisk) PolymarketDisplayMode.GROUPED_OUTCOMES else PolymarketDisplayMode.PLAIN_MARKETS
        }
    }
}