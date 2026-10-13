package com.tangem.data.polymarket

import com.tangem.grow.datasource.polymarket.socket.models.SportsResultDto
import com.tangem.utils.logging.TangemLogger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal class PolymarketSportsFeedStats(
    private val trackedGameIds: Set<Long>,
    private val summaryInterval: Duration = 60.seconds,
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) {

    private val startedAt = timeSource.markNow()
    private var lastSummaryAt = startedAt

    private var framesCount = 0
    private var matchedFramesCount = 0
    private val seenGameIds = mutableSetOf<Long>()
    private val matchedGameIds = mutableSetOf<Long>()
    private val framesByLeague = mutableMapOf<String, Int>()

    fun record(result: SportsResultDto) {
        val isMatched = result.gameId in trackedGameIds

        framesCount++
        seenGameIds += result.gameId
        framesByLeague.merge(result.leagueAbbreviation ?: "?", 1, Int::plus)
        if (isMatched) {
            matchedFramesCount++
            matchedGameIds += result.gameId
        }

        TangemLogger.d(
            "Sports socket: frame gameId=${result.gameId} league=${result.leagueAbbreviation} " +
                "status=${result.status} score=${result.score} period=${result.period} matched=$isMatched",
        )

        if (lastSummaryAt.elapsedNow() >= summaryInterval) {
            TangemLogger.i(summary(reason = "periodic"))
            lastSummaryAt = timeSource.markNow()
        }
    }

    fun logStopped() {
        TangemLogger.i(summary(reason = "stopped"))
    }

    private fun summary(reason: String): String {
        val elapsedSeconds = startedAt.elapsedNow().inWholeMilliseconds / MILLIS_IN_SECOND
        val rate = if (elapsedSeconds > 0) framesCount / elapsedSeconds else 0.0
        return "Sports socket stats ($reason): elapsed=${"%.0f".format(elapsedSeconds)}s, " +
            "frames=$framesCount (${"%.2f".format(rate)}/s), " +
            "matched frames=$matchedFramesCount, games seen=${seenGameIds.size}, " +
            "games matched=${matchedGameIds.size} of tracked=${trackedGameIds.size}, " +
            "frames by league=${framesByLeague.entries.sortedByDescending { it.value }.joinToString()}"
    }

    private companion object {
        const val MILLIS_IN_SECOND = 1000.0
    }
}