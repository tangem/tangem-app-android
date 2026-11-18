package com.tangem.features.polymarket.impl.soccer.model

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketSoccerCard
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent

internal fun PolymarketSoccerEvent.withMainEvent(update: PolymarketEvent): PolymarketSoccerEvent {
    val markets = update.markets.associateBy { it.id }
    return copy(
        status = update.status,
        isLive = update.isLive,
        // Only matched Moneyline prices may receive a new timestamp in the chart.
        dataFetchedAt = if (hasCurrentMoneyline(update)) update.dataFetchedAt else dataFetchedAt,
        tabs = tabs.map { tab ->
            tab.copy(cards = tab.cards.map { card ->
                card.copy(markets = card.markets.map { market ->
                    markets[market.id]?.takeIf(market::canUpdateFrom) ?: market
                })
            })
        },
    )
}

private fun PolymarketMarket.canUpdateFrom(next: PolymarketMarket?): Boolean =
    next != null && next.eventId == eventId && next.version == version &&
        next.outcomes.map { it.assetId }.toSet() == outcomes.map { it.assetId }.toSet()

internal fun PolymarketSoccerEvent.hasCurrentMoneyline(update: PolymarketEvent): Boolean {
    val moneyline = tabs.find { it.key == PolymarketSoccerTab.Key.GAME_LINES }
        ?.cards?.find { it.key == PolymarketSoccerCard.Key.MONEYLINE } ?: return false
    return moneyline.markets.all { market -> market.canUpdateFrom(update.markets.find { it.id == market.id }) }
}