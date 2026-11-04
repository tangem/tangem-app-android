package com.tangem.domain.polymarket.model

import java.util.Locale

data class PolymarketSportsState(
    /** Indexed by event id, so several feed events may refer to the same game. */
    val events: Map<String, Game>,
    /** A changed nonzero revision asks the caller to refetch REST, independently of score changes. */
    val resnapshotRevision: Long = 0,
) {

    data class Game(
        val gameId: String,
        val score: Score?,
        val period: String?,
        val isLive: Boolean,
        val isEnded: Boolean,
        val home: Team?,
        val away: Team?,
        val isStale: Boolean,
    ) {
        val shouldShowLiveBadge: Boolean
            get() = isLive && !isEnded && period?.uppercase(Locale.ROOT) !in NO_LIVE_PERIODS

        private companion object {
            val NO_LIVE_PERIODS = setOf("CAN", "POST", "INT", "AB")
        }
    }

    data class Score(val home: Int, val away: Int)

    /** Retains the REST team index for matching price buttons without reordering them independently. */
    data class Team(val value: PolymarketTeam, val restIndex: Int?)
}