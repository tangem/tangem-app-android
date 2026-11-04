package com.tangem.features.polymarket.impl.main.model.converter

import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.model.PolymarketTeam
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import com.tangem.features.polymarket.impl.common.PolymarketTeamMatcher
import com.tangem.features.polymarket.impl.common.formatPolymarketPrice
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventRowUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketOutcomeUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketSportsUM
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.util.Locale

internal class PolymarketSportsUMConverter(
    private val isTeamAbbreviationEnabled: Boolean = false,
    private val onOutcomeClick: (eventId: String, marketId: String, assetId: String) -> Unit,
) {

    fun convert(game: PolymarketSportsState.Game, event: PolymarketEvent): PolymarketSportsUM {
        val teams = event.teams.orEmpty()
        val score = game.score.takeIf { teams.size == 2 }
        val orderedScore = when (teams.firstOrNull()?.ordering?.lowercase(Locale.ROOT)) {
            "home" -> score
            "away" -> score?.let { PolymarketSportsState.Score(it.away, it.home) }
            else -> null
        }
        return PolymarketSportsUM(
            home = teams.firstOrNull { it.isOnSide("home") }?.toTeam(),
            away = teams.firstOrNull { it.isOnSide("away") }?.toTeam(),
            homeScore = orderedScore?.home?.toString(),
            awayScore = orderedScore?.away?.toString(),
            period = game.period?.takeIf(String::isNotBlank),
            isLive = game.shouldShowLiveBadge,
        )
    }

    fun outcomes(event: PolymarketEvent, game: PolymarketSportsState.Game?): PolymarketEventRowUM? {
        if (game == null || !game.isLive && !game.isEnded) return null
        val teams = event.teams.orEmpty()
        val home = teams.firstOrNull { it.isOnSide("home") } ?: return null
        val away = teams.firstOrNull { it.isOnSide("away") } ?: return null
        val ordered = moneylineChoices(event = event, home = home, away = away) ?: return null
        val titles = teamTitles(listOf(home, away))
        return PolymarketEventRowUM(
            marketId = ordered.first().marketId,
            title = null,
            probability = null,
            outcomes = ordered.map { choice ->
                val (title, accent) = when (choice) {
                    ordered.first() -> titles[0] to PolymarketOutcomeAccent.Info
                    ordered.last() -> titles[1] to PolymarketOutcomeAccent.Error
                    else -> "Draw" to PolymarketOutcomeAccent.Neutral
                }
                PolymarketOutcomeUM(
                    assetId = choice.assetId,
                    title = stringReference(title),
                    price = stringReference(choice.price.formatPolymarketPrice()),
                    accent = accent,
                    onClick = { onOutcomeClick(event.id, choice.marketId, choice.assetId) },
                )
            }.toImmutableList(),
        )
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

    private fun teamTitles(teams: List<PolymarketTeam>): List<String> {
        val abbreviations = teams.map { it.abbreviation?.trim()?.takeIf(String::isNotEmpty)?.uppercase(Locale.ROOT) }
        return if (isTeamAbbreviationEnabled && abbreviations.all { it != null }) {
            abbreviations.filterNotNull()
        } else {
            teams.map { it.name }
        }
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

    private fun PolymarketTeam.isOnSide(side: String): Boolean = ordering.equals(side, ignoreCase = true)

    private companion object {
        const val GROUPED_MARKETS_COUNT = 3
    }

    private data class Choice(val marketId: String, val assetId: String, val label: String, val price: BigDecimal?)
}