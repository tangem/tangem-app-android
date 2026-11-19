package com.tangem.domain.polymarket.sports

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PolymarketTeam
import java.time.Instant

internal data class PolymarketSportsEvent(
    val id: String,
    val gameId: String?,
    val teams: List<PolymarketTeam>?,
    val score: String?,
    val period: String?,
    val isLive: Boolean,
    val isEnded: Boolean?,
    val dataFetchedAt: Instant?,
) {
    companion object {
        fun from(event: PolymarketEvent) = PolymarketSportsEvent(
            id = event.id,
            gameId = event.gameId,
            teams = event.teams,
            score = event.score,
            period = event.period,
            isLive = event.isLive,
            isEnded = event.isEnded,
            dataFetchedAt = event.dataFetchedAt,
        )

        fun from(event: PolymarketSoccerEvent) = PolymarketSportsEvent(
            id = event.id,
            gameId = event.gameId,
            teams = event.teams,
            score = event.score,
            period = event.period,
            isLive = event.isLive,
            isEnded = event.isEnded,
            dataFetchedAt = event.dataFetchedAt,
        )
    }
}