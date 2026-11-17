package com.tangem.features.polymarket.impl.soccer.model

import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import com.tangem.features.polymarket.impl.R
import com.tangem.features.polymarket.impl.common.PolymarketEventFormatting
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketSportsUM
import com.tangem.features.polymarket.impl.common.formatPolymarketVolume
import com.tangem.features.polymarket.impl.common.toResolutionSourceUM
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerUM
import kotlinx.collections.immutable.toImmutableList

internal class PolymarketSoccerConverter(private val onTabClick: (PolymarketSoccerTab.Key) -> Unit) {
    private val cards = PolymarketSoccerCardConverter

    fun convert(event: PolymarketSoccerEvent, selection: PolymarketSoccerSelection): PolymarketSoccerUM.Content {
        val score = event.displayScore
        val teams = event.teams.map { PolymarketSportsUM.Team(stringReference(it.name), it.logoUrl) }
        return PolymarketSoccerUM.Content(
            title = stringReference(event.title),
            league = stringReference(event.league.name),
            kickoff = event.startTime?.let { startTime ->
                stringReference(PolymarketEventFormatting.formatEasternDate(startTime.toString()))
            },
            volume = stringReference(event.volume.formatPolymarketVolume()),
            change24h = PolymarketEventFormatting.transformChange24h(event.volume, event.volume24h),
            sports = PolymarketSportsUM(
                home = teams.getOrNull(0),
                away = teams.getOrNull(1),
                homeScore = score?.left?.toString(),
                awayScore = score?.right?.toString(),
                period = event.period,
                isLive = event.isLive,
            ),
            tabs = event.tabs.map { tab ->
                TangemTabItemUM.Content(
                    id = tab.key.value,
                    label = resourceReference(tab.key.title()),
                    isSelected = tab.key == selection.tab,
                    onClick = { onTabClick(tab.key) },
                )
            }.toImmutableList(),
            cards = event.tabs.find { it.key == selection.tab }?.let { tab ->
                tab.cards.mapNotNull { card ->
                    cards.convert(
                        event = event,
                        tab = tab.key,
                        card = card,
                        selectedMarketId = selection.positions[card.id(tab.key)],
                    )
                }
            }.orEmpty().toImmutableList(),
            description = event.description.takeIf(String::isNotBlank)?.let(::stringReference),
            isDescriptionExpanded = false,
            resolutionDate = stringReference(PolymarketEventFormatting.formatEasternDate(event.endDate)),
            marketOpenedDate = stringReference(PolymarketEventFormatting.formatEasternDate(event.startDate)),
            resolutionSource = event.resolutionSource.toResolutionSourceUM(),
        )
    }

    private fun PolymarketSoccerTab.Key.title(): Int = when (this) {
        PolymarketSoccerTab.Key.GAME_LINES -> R.string.prediction_match_tab_game_lines
        PolymarketSoccerTab.Key.EXACT_SCORE -> R.string.prediction_match_tab_exact_score
        PolymarketSoccerTab.Key.FIRST_HALF -> R.string.prediction_match_tab_first_half
        PolymarketSoccerTab.Key.SECOND_HALF -> R.string.prediction_match_tab_second_half
    }
}