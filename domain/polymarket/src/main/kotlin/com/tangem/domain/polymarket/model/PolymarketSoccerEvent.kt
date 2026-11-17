package com.tangem.domain.polymarket.model

import com.tangem.domain.polymarket.sports.PolymarketScoreParser
import java.math.BigDecimal
import java.time.Instant

/** Soccer match returned for either the main event ID or any child event ID. */
data class PolymarketSoccerEvent(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val rulesUrl: String,
    val imageUrl: String?,
    val iconUrl: String?,
    val status: PolymarketStatus,
    val isLive: Boolean,
    /** Kickoff time. The upstream startDate is the event creation date. */
    val startTime: Instant?,
    val startDate: String?,
    val endDate: String?,
    val volume: BigDecimal?,
    val volume24h: BigDecimal?,
    val liquidity: BigDecimal?,
    val isNegRisk: Boolean,
    val gameId: String?,
    val score: String?,
    val period: String?,
    val elapsed: String?,
    val isEnded: Boolean?,
    val dataFetchedAt: Instant?,
    val league: League,
    val resolutionSource: String?,
    val sportOrdering: String?,
    /** BFF display order: left, right. */
    val teams: List<PolymarketTeam>,
    val tabs: List<PolymarketSoccerTab>,
) {
    /** Scores follow the same BFF display order as [teams]. */
    val displayScore: Score?
        get() = PolymarketScoreParser.parse(score)?.let { Score(left = it.first, right = it.second) }

    data class Score(val left: Int, val right: Int)

    data class League(val slug: String, val name: String, val imageUrl: String?)
}

data class PolymarketSoccerTab(val key: Key, val cards: List<PolymarketSoccerCard>) {
    enum class Key(val value: String) {
        GAME_LINES("game_lines"), EXACT_SCORE("exact_score"), FIRST_HALF("first_half"), SECOND_HALF("second_half"),
    }
}