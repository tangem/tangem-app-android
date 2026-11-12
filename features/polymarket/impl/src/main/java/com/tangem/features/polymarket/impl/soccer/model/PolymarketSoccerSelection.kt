package com.tangem.features.polymarket.impl.soccer.model

import com.tangem.domain.polymarket.model.PolymarketSoccerCard
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PolymarketSoccerTab

internal data class PolymarketSoccerSelection(
    val tab: PolymarketSoccerTab.Key? = null,
    /** Only explicit user choices override the BFF default on subsequent snapshots. */
    val positions: Map<String, String> = emptyMap(),
) {
    fun reconcile(event: PolymarketSoccerEvent): PolymarketSoccerSelection {
        val selectedPositions = buildMap {
            for (tab in event.tabs) {
                for (card in tab.cards) {
                    val id = card.id(tab.key)
                    val selected = positions[id] ?: continue
                    if (card.pickerPositions()?.any { it.marketId == selected } == true) put(id, selected)
                }
            }
        }
        return copy(
            tab = tab?.takeIf { selected -> event.tabs.any { it.key == selected } } ?: event.tabs.firstOrNull()?.key,
            positions = selectedPositions,
        )
    }

    fun selectTab(event: PolymarketSoccerEvent, tab: PolymarketSoccerTab.Key): PolymarketSoccerSelection {
        return if (event.tabs.any { it.key == tab }) copy(tab = tab) else this
    }

    fun selectPosition(event: PolymarketSoccerEvent, cardId: String, marketId: String): PolymarketSoccerSelection {
        val card = event.tabs.firstNotNullOfOrNull { tab -> tab.cards.find { it.id(tab.key) == cardId } }
        val choices = card?.pickerPositions() ?: return this
        if (choices.none { it.marketId == marketId }) return this
        return copy(positions = positions + (cardId to marketId))
    }
}

internal fun PolymarketSoccerCard.id(tab: PolymarketSoccerTab.Key): String {
    val side = (content as? PolymarketSoccerCard.Content.OverUnderPicker)?.teamSide
    return "${tab.value}/${key.value}" + if (side == null) "" else "/${side.name}"
}

internal fun PolymarketSoccerCard.pickerPositions(): List<PolymarketSoccerCard.Position>? =
    when (val content = content) {
        is PolymarketSoccerCard.Content.SidePicker -> content.positions
        is PolymarketSoccerCard.Content.OverUnderPicker -> content.positions
        else -> null
    }

internal fun PolymarketSoccerCard.selectedPosition(marketId: String?): PolymarketSoccerCard.Position? {
    val positions = pickerPositions() ?: return null
    return positions.find { it.marketId == marketId } ?: positions.find { it.isDefault }
}