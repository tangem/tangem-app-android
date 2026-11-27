package com.tangem.features.polymarket.impl.soccer.model

import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.features.polymarket.impl.common.PolymarketTeamMatcher
import com.tangem.features.polymarket.impl.common.feed.model.converter.toSportsUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketSportsUM

internal fun PolymarketSoccerEvent.sportsUM(game: PolymarketSportsState.Game?): PolymarketSportsUM {
    val teams = teams.map { PolymarketSportsUM.Team(stringReference(it.name), it.logoUrl) }
    val baseline = PolymarketSportsUM(
        home = teams.getOrNull(0),
        away = teams.getOrNull(1),
        homeScore = displayScore?.left?.toString(),
        awayScore = displayScore?.right?.toString(),
        period = period,
        isLive = isLive && isEnded != true,
    )
    if (game == null || game.gameId != gameId) return baseline
    val merged = game.toSportsUM()
    val homeIndex = game.home?.value?.let { PolymarketTeamMatcher.find(it.name, this.teams) }
        ?.let(this.teams::indexOf)
    val awayIndex = game.away?.value?.let { PolymarketTeamMatcher.find(it.name, this.teams) }
        ?.let(this.teams::indexOf)
    // REST event updates may order teams differently from the soccer layout and its price buttons.
    return merged.copy(
        home = baseline.home,
        away = baseline.away,
        homeScore = when (0) {
            homeIndex -> merged.homeScore
            awayIndex -> merged.awayScore
            else -> null
        },
        awayScore = when (1) {
            homeIndex -> merged.homeScore
            awayIndex -> merged.awayScore
            else -> null
        },
    )
}