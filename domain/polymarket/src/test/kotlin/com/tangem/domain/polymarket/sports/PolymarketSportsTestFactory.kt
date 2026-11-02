package com.tangem.domain.polymarket.sports

import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.model.PolymarketTeam
import java.time.Instant

internal fun createSportsEvent(
    id: String = "event",
    gameId: String? = "1",
    score: String? = "0-0",
    period: String? = "Q1",
    live: Boolean = true,
    ended: Boolean? = false,
    fetchedAt: Instant? = Instant.ofEpochSecond(100),
    teams: List<PolymarketTeam>? = emptyList(),
) = PolymarketEvent(
    id = id, slug = id, title = "Match", description = "", rulesUrl = "", iconUrl = null, imageUrl = null,
    status = PolymarketStatus.ACTIVE, startDate = null, endDate = null, volume = null, volume24h = null,
    liquidity = null, totalMarketsCount = 0, isNegRisk = false, isLive = live,
    displayMode = PolymarketDisplayMode.PLAIN_MARKETS, markets = emptyList(), gameId = gameId,
    score = score, period = period, isEnded = ended, dataFetchedAt = fetchedAt, teams = teams,
)

internal fun createSportsScore(
    gameId: String = "1",
    score: String? = "1-0",
    period: String? = "Q2",
    live: Boolean? = true,
    ended: Boolean? = false,
    timestamp: Long = 110,
) = PolymarketLiveScore(
    gameId, score, period, live, ended, updatedAt = Instant.ofEpochSecond(timestamp),
    lastSeenAt = Instant.ofEpochSecond(timestamp),
)

internal fun createSportsTeam(name: String = "Team", ordering: String? = null) =
    PolymarketTeam(name, name.take(3), null, "logo/$name", ordering)