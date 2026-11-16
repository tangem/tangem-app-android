package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import com.tangem.utils.converter.Converter
import com.tangem.grow.datasource.polymarket.models.PolymarketSoccerEventDto
import java.math.BigDecimal
import java.time.Instant

internal object PolymarketSoccerEventConverter : Converter<PolymarketSoccerEventDto, PolymarketSoccerEvent> {
    override fun convert(value: PolymarketSoccerEventDto): PolymarketSoccerEvent = PolymarketSoccerEvent(
        id = value.eventId,
        slug = value.slug,
        title = value.title,
        description = value.description,
        rulesUrl = value.polymarketRulesUrl,
        imageUrl = value.image,
        iconUrl = value.icon,
        status = PolymarketStatusConverter.convert(value.status),
        isLive = value.isLive,
        startTime = value.startTime?.let(Instant::parse),
        startDate = value.startDate,
        endDate = value.endDate,
        volume = value.volume?.let(BigDecimal::valueOf),
        volume24h = value.volume24hr?.let(BigDecimal::valueOf),
        liquidity = value.liquidity?.let(BigDecimal::valueOf),
        isNegRisk = value.isNegRisk,
        gameId = value.gameId,
        score = value.score,
        period = value.period,
        elapsed = value.elapsed?.takeIf(String::isNotBlank),
        isEnded = value.isEnded,
        dataFetchedAt = value.dataFetchedAt?.let(Instant::parse),
        league = PolymarketSoccerEvent.League(value.league.slug, value.league.name, value.league.image),
        resolutionSource = value.resolutionSource,
        sportOrdering = value.sportOrdering,
        teams = value.teams.map(PolymarketTeamConverter::convert),
        tabs = value.tabs.mapNotNull { tab ->
            val key = PolymarketSoccerTab.Key.entries.find { it.value == tab.key } ?: return@mapNotNull null
            val cards = tab.cards.mapNotNull(PolymarketSoccerCardConverter::convert)
            cards.takeIf { it.isNotEmpty() }?.let { PolymarketSoccerTab(key, it) }
        },
    )
}