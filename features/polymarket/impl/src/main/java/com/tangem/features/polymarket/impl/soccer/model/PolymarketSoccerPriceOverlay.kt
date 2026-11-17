package com.tangem.features.polymarket.impl.soccer.model

import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.polymarket.impl.common.formatPolymarketPrice
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerCardUM
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerUM
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal fun PolymarketSoccerUM.Content.withPrices(prices: Map<String, BigDecimal?>): PolymarketSoccerUM.Content {
    if (prices.isEmpty()) return this
    return copy(cards = cards.map { card ->
        val content = card.content
        val outcomes = when (content) {
            is PolymarketSoccerCardUM.Content.Buttons -> content.outcomes
            is PolymarketSoccerCardUM.Content.Rows -> content.outcomes
            is PolymarketSoccerCardUM.Content.Picker -> content.outcomes
        }.map { outcome ->
            val id = outcome.target.assetId
            if (prices.containsKey(id)) {
                outcome.copy(price = stringReference(prices[id].formatPolymarketPrice()))
            } else {
                outcome
            }
        }.toImmutableList()
        card.copy(
            content = when (content) {
                is PolymarketSoccerCardUM.Content.Buttons -> content.copy(outcomes = outcomes)
                is PolymarketSoccerCardUM.Content.Rows -> content.copy(outcomes = outcomes)
                is PolymarketSoccerCardUM.Content.Picker -> content.copy(outcomes = outcomes)
            },
        )
    }.toImmutableList())
}