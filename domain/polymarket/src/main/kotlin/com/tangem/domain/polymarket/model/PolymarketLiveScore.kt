package com.tangem.domain.polymarket.model

/**
 * @property gameId the [PolymarketEvent.gameId] this update belongs to
 * @property score raw provider score, in the same format as [PolymarketEvent.score]
 * @property period raw provider period, in the same format as [PolymarketEvent.period]
 */
data class PolymarketLiveScore(
    val gameId: String,
    val score: String?,
    val period: String?,
    val isLive: Boolean?,
    val isEnded: Boolean?,
)