package com.tangem.features.polymarket.impl.main.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.features.polymarket.impl.common.buyPrice
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.model.PolymarketTeam
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeButtonTitle
import com.tangem.features.polymarket.impl.common.PolymarketTeamMatcher
import com.tangem.features.polymarket.impl.common.formatPolymarketPrice
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventRowUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketOutcomeUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketSportsUM
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.util.Locale

internal class PolymarketSportsUMConverter(
    private val isTeamAbbreviationEnabled: Boolean,
    private val onOutcomeClick: (eventId: String, marketId: String, assetId: String) -> Unit,
) {

    fun convert(game: PolymarketSportsState.Game): PolymarketSportsUM = PolymarketSportsUM(
        home = game.home?.value?.toTeam(),
        away = game.away?.value?.toTeam(),
        homeScore = game.score?.home?.toString(),
        awayScore = game.score?.away?.toString(),
        period = game.period?.takeIf(String::isNotBlank),
        isLive = game.isLive && !game.isEnded && game.period?.uppercase(Locale.ROOT) !in NO_LIVE_PERIODS,
    )

    fun outcomes(
        event: PolymarketEvent,
        game: PolymarketSportsState.Game?,
        quotes: Map<String, PolymarketLiveQuote> = emptyMap(),
    ): PolymarketEventRowUM? {
        if (game == null || !game.isLive && !game.isEnded) return null
        val home = restTeam(event, game.home) ?: return null
        val away = restTeam(event, game.away) ?: return null
        if (home == away) return null
        val ordered = moneylineChoices(event = event, home = home, away = away) ?: return null
        val titles = PolymarketOutcomeButtonTitle.convert(
            listOf(home.name, away.name),
            listOf(home, away),
            isTeamAbbreviationEnabled,
        )
        return PolymarketEventRowUM(
            marketId = ordered.first().marketId,
            title = null,
            probability = null,
            outcomes = ordered.map { choice ->
                val (title, accent) = when (choice) {
                    ordered.first() -> stringReference(titles[0]) to PolymarketOutcomeAccent.Info
                    ordered.last() -> stringReference(titles[1]) to PolymarketOutcomeAccent.Error
                    else -> resourceReference(R.string.prediction_sports_draw) to PolymarketOutcomeAccent.Neutral
                }
                PolymarketOutcomeUM(
                    assetId = choice.assetId,
                    title = title,
                    price = stringReference(quotes[choice.assetId].buyPrice(choice.price).formatPolymarketPrice()),
                    accent = accent,
                    onClick = { onOutcomeClick(event.id, choice.marketId, choice.assetId) },
                )
            }.toImmutableList(),
        )
    }

    private fun restTeam(event: PolymarketEvent, side: PolymarketSportsState.Team?): PolymarketTeam? {
        val index = side?.restIndex ?: return null
        return event.teams?.getOrNull(index)?.takeIf { it.name == side.value.name }
    }

    private fun moneylineChoices(event: PolymarketEvent, home: PolymarketTeam, away: PolymarketTeam): List<Choice>? {
        val choices = choices(event)
        val homeChoice = choices.firstOrNull {
            PolymarketTeamMatcher.find(it.label, event.teams) == home
        } ?: return null
        val awayChoice = choices.firstOrNull {
            PolymarketTeamMatcher.find(it.label, event.teams) == away
        } ?: return null
        val draw = if (choices.size == GROUPED_MARKETS_COUNT) {
            if (homeChoice.marketId == awayChoice.marketId) return null
            choices.firstOrNull { it.marketId != homeChoice.marketId && it.marketId != awayChoice.marketId }
                ?.takeIf { it.label.startsWith("Draw (") } ?: return null
        } else {
            null
        }
        return listOfNotNull(homeChoice, draw, awayChoice)
    }

    private fun choices(event: PolymarketEvent): List<Choice> {
        if (event.totalMarketsCount > event.markets.size) return emptyList()
        val market = event.markets.singleOrNull()
        if (market != null) {
            if (market.outcomes.size != 2) return emptyList()
            return market.outcomes.map {
                Choice(marketId = market.id, assetId = it.assetId, label = it.title, price = it.probability)
            }
        }
        if (event.markets.size != GROUPED_MARKETS_COUNT) return emptyList()
        return event.markets.map { item ->
            val label = item.groupItemTitle ?: return emptyList()
            val yes = item.outcomes.firstOrNull()?.takeIf {
                item.outcomes.size == 2 && it.title.trim().equals("Yes", ignoreCase = true)
            } ?: return emptyList()
            Choice(marketId = item.id, assetId = yes.assetId, label = label, price = yes.probability)
        }
    }

    private fun PolymarketTeam.toTeam() = PolymarketSportsUM.Team(stringReference(name), logoUrl)

    private companion object {
        const val GROUPED_MARKETS_COUNT = 3
        val NO_LIVE_PERIODS = setOf("CAN", "POST", "INT", "AB")
    }

    private data class Choice(val marketId: String, val assetId: String, val label: String, val price: BigDecimal?)
}