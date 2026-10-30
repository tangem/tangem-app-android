package com.tangem.domain.polymarket.sports

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.domain.polymarket.model.PolymarketSportsState
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class PolymarketSportsMerger(private val time: TimeSource) {

    private var events = emptyMap<String, PolymarketEvent>()
    private val scores = mutableMapOf<String, Entry>()
    private var resnapshotRevision = 0L
    private var hasConnected = false

    fun setEvents(value: List<PolymarketEvent>): Set<String> {
        events = value.filter { it.gameId?.toLongOrNull() != null }.associate { incoming ->
            val previous = events[incoming.id]
            val shouldKeepPrevious = previous?.gameId == incoming.gameId && previous?.dataFetchedAt != null &&
                (incoming.dataFetchedAt == null || incoming.dataFetchedAt < previous.dataFetchedAt)
            incoming.id to if (shouldKeepPrevious) requireNotNull(previous) else incoming
        }
        val gameIds = events.values.mapNotNullTo(mutableSetOf(), PolymarketEvent::gameId)
        scores.keys.retainAll(gameIds)
        return gameIds
    }

    fun update(value: PolymarketLiveScore) {
        if (events.values.none { it.gameId == value.gameId }) return
        val previous = latestScore(value.gameId)
        val score = if (previous != null && value.updatedAt < previous.updatedAt) {
            previous.copy(lastSeenAt = value.lastSeenAt)
        } else {
            value.copy(
                score = value.score ?: previous?.score,
                period = value.period ?: previous?.period,
                isLive = value.isLive ?: previous?.isLive,
                isEnded = value.isEnded ?: previous?.isEnded,
                homeTeam = value.homeTeam ?: previous?.homeTeam,
                awayTeam = value.awayTeam ?: previous?.awayTeam,
                updatedAt = if (hasState(value)) value.updatedAt else previous?.updatedAt ?: value.updatedAt,
            )
        }
        scores[value.gameId] = Entry(score, time.markNow())
    }

    private fun latestScore(gameId: String): PolymarketLiveScore? {
        val previous = scores[gameId]?.score
        val rest = events.values.filter { it.gameId == gameId }.maxByOrNull { it.dataFetchedAt ?: Instant.MIN }
        val fetchedAt = rest?.dataFetchedAt ?: return previous
        if (previous != null && (previous.updatedAt >= fetchedAt || keepsFinalResult(previous, rest))) return previous
        return PolymarketLiveScore(
            gameId = gameId,
            score = rest.score,
            period = rest.period,
            isLive = rest.isLive,
            isEnded = rest.isEnded,
            homeTeam = previous?.homeTeam,
            awayTeam = previous?.awayTeam,
            updatedAt = fetchedAt,
        )
    }

    private fun hasState(score: PolymarketLiveScore): Boolean =
        score.score != null || score.period != null || score.isLive != null || score.isEnded != null

    fun connected() {
        if (hasConnected) resnapshotRevision++
        hasConnected = true
    }

    fun snapshot(): PolymarketSportsState = PolymarketSportsState(
        events = events.mapValues { (_, event) -> game(event) },
        resnapshotRevision = resnapshotRevision,
    )

    private fun game(event: PolymarketEvent): PolymarketSportsState.Game {
        val entry = scores[event.gameId]
        val score = entry?.score
        val isStale = isStale(entry, event)
        val shouldUseSocket = score != null && !isStale &&
            (event.dataFetchedAt == null || score.updatedAt >= event.dataFetchedAt || keepsFinalResult(score, event))
        val selected = score.takeIf { shouldUseSocket }
        val isLive = selected?.isLive ?: event.isLive
        val isEnded = selected?.isEnded ?: event.isEnded == true
        val period = selected?.period ?: event.period
        val (home, away) = PolymarketTeamSides.resolve(event.teams.orEmpty(), score?.homeTeam, score?.awayTeam)
        return PolymarketSportsState.Game(
            gameId = requireNotNull(event.gameId),
            score = if (isLive || isEnded) {
                PolymarketScoreParser.parse(selected?.score ?: event.score, period)
            } else {
                null
            },
            period = period,
            isLive = isLive && !isEnded && period !in NO_LIVE_PERIODS,
            isEnded = isEnded,
            home = home,
            away = away,
            isStale = isStale,
        )
    }

    private fun keepsFinalResult(score: PolymarketLiveScore, event: PolymarketEvent): Boolean =
        score.isEnded == true && event.isEnded != true

    private fun isStale(entry: Entry?, event: PolymarketEvent): Boolean {
        if (entry == null) return false
        val isLive = entry.score.isLive ?: event.isLive
        val isEnded = entry.score.isEnded ?: event.isEnded == true
        return isLive && !isEnded && entry.lastSeen.elapsedNow() > STALE_AFTER
    }

    private data class Entry(val score: PolymarketLiveScore, val lastSeen: TimeMark)

    private companion object {
        val STALE_AFTER = 60.seconds
        val NO_LIVE_PERIODS = setOf("CAN", "POST", "INT", "AB")
    }
}