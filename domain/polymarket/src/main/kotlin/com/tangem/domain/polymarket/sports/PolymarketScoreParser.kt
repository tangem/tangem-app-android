package com.tangem.domain.polymarket.sports

import kotlin.math.abs

internal object PolymarketScoreParser {

    private val PAIR = Regex("(\\d+)\\s*-\\s*(\\d+)")
    private val SET = Regex("(\\d+\\s*-\\s*\\d+)(?:\\s*\\(\\d+(?:\\s*-\\s*\\d+)?\\))?")
    private const val GAMES_TO_WIN = 6
    private const val TIEBREAK_WIN = 7
    private const val WIN_MARGIN = 2

    fun parse(raw: String?, period: String? = null): ProviderScore? {
        if (raw == null) return null
        return when {
            '|' in raw -> pair(raw.split('|').getOrNull(1))
            ',' in raw || '(' in raw || isTennisPeriod(period) -> tennis(raw)
            else -> pair(raw)
        }
    }

    data class ProviderScore(val first: Int, val second: Int)

    private fun isTennisPeriod(period: String?): Boolean =
        period?.startsWith("S") == true && period.drop(1).toIntOrNull() ?: 0 > 0

    private fun pair(raw: String?): ProviderScore? {
        val match = raw?.trim()?.let(PAIR::matchEntire) ?: return null
        val first = match.groupValues[1].toIntOrNull() ?: return null
        val second = match.groupValues[2].toIntOrNull() ?: return null
        return ProviderScore(first, second)
    }

    private fun tennis(raw: String): ProviderScore? {
        var first = 0
        var second = 0
        raw.split(',').forEach { set ->
            val match = SET.matchEntire(set.trim()) ?: return null
            val score = pair(match.groupValues[1]) ?: return null
            val winner = maxOf(score.first, score.second)
            val loser = minOf(score.first, score.second)
            val isFinished = winner >= GAMES_TO_WIN && abs(score.first - score.second) >= WIN_MARGIN ||
                winner == TIEBREAK_WIN && loser == GAMES_TO_WIN
            if (isFinished) {
                if (score.first > score.second) first++ else second++
            }
        }
        return ProviderScore(first, second)
    }
}