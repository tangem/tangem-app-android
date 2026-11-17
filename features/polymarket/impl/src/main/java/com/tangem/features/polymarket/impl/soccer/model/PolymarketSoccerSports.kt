package com.tangem.features.polymarket.impl.soccer.model

import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PolymarketSportsState
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
    // The header follows the same BFF display order as the market buttons, not home/away order.
    return merged.copy(
        home = baseline.home,
        away = baseline.away,
        homeScore = when (0) {
            game.home?.restIndex -> merged.homeScore
            game.away?.restIndex -> merged.awayScore
            else -> null
        },
        awayScore = when (1) {
            game.home?.restIndex -> merged.homeScore
            game.away?.restIndex -> merged.awayScore
            else -> null
        },
    )
}