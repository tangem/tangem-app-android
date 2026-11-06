package com.tangem.domain.polymarket.model

import java.time.Instant

/**
 * @property gameId the [PolymarketEvent.gameId] this update belongs to
 * @property score raw provider score, in the same format as [PolymarketEvent.score]
 * @property period raw provider period, in the same format as [PolymarketEvent.period]
 * @property updatedAt source timestamp, or reception time when the provider timestamp is unavailable
 * @property lastSeenAt reception time, refreshed even for an unchanged broadcast
 */
data class PolymarketLiveScore(
    val gameId: String,
    val score: String?,
    val period: String?,
    val isLive: Boolean?,
    val isEnded: Boolean?,
    val homeTeam: String? = null,
    val awayTeam: String? = null,
    val updatedAt: Instant = Instant.EPOCH,
    val lastSeenAt: Instant = updatedAt,
)