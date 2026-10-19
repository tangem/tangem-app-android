package com.tangem.domain.polymarket.sports

import com.tangem.domain.polymarket.model.PolymarketSportsState.Score
import kotlin.math.abs

internal object PolymarketScoreParser {

    private val PAIR = Regex("(\\d+)\\s*-\\s*(\\d+)")
    private val SET = Regex("(\\d+\\s*-\\s*\\d+)(?:\\s*\\(\\d+(?:\\s*-\\s*\\d+)?\\))?")
    private const val GAMES_TO_WIN = 6
    private const val TIEBREAK_WIN = 7
    private const val WIN_MARGIN = 2

    fun parse(raw: String?, period: String? = null): Score? {
        if (raw == null) return null
        return when {
            '|' in raw -> pair(raw.split('|').getOrNull(1))
            ',' in raw || '(' in raw || isTennisPeriod(period) -> tennis(raw)
            else -> pair(raw)
        }
    }

    private fun isTennisPeriod(period: String?): Boolean =
        period?.startsWith("S") == true && period.drop(1).toIntOrNull() ?: 0 > 0

    private fun pair(raw: String?): Score? {
        val match = raw?.trim()?.let(PAIR::matchEntire) ?: return null
        val home = match.groupValues[1].toIntOrNull() ?: return null
        val away = match.groupValues[2].toIntOrNull() ?: return null
        return Score(home, away)
    }

    private fun tennis(raw: String): Score? {
        var home = 0
        var away = 0
        raw.split(',').forEach { set ->
            val match = SET.matchEntire(set.trim()) ?: return null
            val score = pair(match.groupValues[1]) ?: return null
            val winner = maxOf(score.home, score.away)
            val loser = minOf(score.home, score.away)
            val isFinished = winner >= GAMES_TO_WIN && abs(score.home - score.away) >= WIN_MARGIN ||
                winner == TIEBREAK_WIN && loser == GAMES_TO_WIN
            if (isFinished) {
                if (score.home > score.away) home++ else away++
            }
        }
        return Score(home, away)
    }
}