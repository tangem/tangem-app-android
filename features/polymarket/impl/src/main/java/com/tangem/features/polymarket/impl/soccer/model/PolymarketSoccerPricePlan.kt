package com.tangem.features.polymarket.impl.soccer.model

import com.tangem.domain.polymarket.model.PolymarketSoccerCard
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import java.math.BigDecimal

internal data class PolymarketSoccerPricePlan(
    val eventId: String,
    val isEnded: Boolean,
    val polled: Set<String>,
    val streamed: Set<String>,
) {
    companion object {
        fun create(event: PolymarketSoccerEvent): PolymarketSoccerPricePlan {
            val all = event.buttonPrices().keys
            val streamed = if (event.isLive && event.isEnded != true) {
                event.tabs.asSequence().filter { it.key == PolymarketSoccerTab.Key.GAME_LINES }
                    .flatMap { it.cards }.filter { it.key == PolymarketSoccerCard.Key.MONEYLINE }
                    .flatMap { it.buttons() }.map { it.assetId }.filter { it in all }.toSet()
            } else {
                emptySet()
            }
            return PolymarketSoccerPricePlan(
                eventId = event.id,
                isEnded = event.isEnded == true,
                polled = if (event.isEnded == true) emptySet() else all - streamed,
                streamed = streamed,
            )
        }
    }
}

internal fun PolymarketSoccerEvent.buttonPrices(): Map<String, BigDecimal?> = buildMap {
    tabs.flatMap { it.cards }.forEach { card ->
        card.buttons().forEach { button ->
            val market = card.markets.find { it.id == button.marketId }
            val outcome = market?.outcomes?.find { it.assetId == button.assetId }
            if (outcome != null && outcome.assetId.isNotBlank()) put(outcome.assetId, outcome.probability)
        }
    }
}

private fun PolymarketSoccerCard.buttons(): List<PolymarketSoccerCard.Button> = when (val content = content) {
    is PolymarketSoccerCard.Content.ThreeWay -> content.buttons
    is PolymarketSoccerCard.Content.YesNo -> content.buttons
    is PolymarketSoccerCard.Content.Rows -> content.rows
    is PolymarketSoccerCard.Content.SidePicker -> content.positions.flatMap { it.buttons }
    is PolymarketSoccerCard.Content.OverUnderPicker -> content.positions.flatMap { it.buttons }
}