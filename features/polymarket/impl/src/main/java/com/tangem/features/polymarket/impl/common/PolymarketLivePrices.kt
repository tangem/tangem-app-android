package com.tangem.features.polymarket.impl.common

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.domain.polymarket.model.PolymarketSportsState
import java.math.BigDecimal

internal fun PolymarketEvent.isLiveForQuotes(game: PolymarketSportsState.Game?): Boolean {
    val currentGame = game?.takeIf { it.gameId == gameId }
    return currentGame?.let { it.isLive && !it.isEnded } ?: (isLive && isEnded != true)
}

internal fun PolymarketLiveQuote?.buyPrice(restPrice: BigDecimal?): BigDecimal? =
    if (this?.status == PolymarketLiveQuote.Status.LIVE) bestAsk else restPrice

/** Feed probability uses the spread threshold; soccer charts consume the uncapped midpoint directly. */
internal fun PolymarketLiveQuote?.probability(restProbability: BigDecimal?): BigDecimal? {
    if (this?.status != PolymarketLiveQuote.Status.LIVE) return restProbability
    val bid = bestBid
    val ask = bestAsk
    val midpoint = midpoint()?.takeIf { bid != null && ask != null && ask - bid <= MAX_MIDPOINT_SPREAD }
    return midpoint ?: lastTradePrice ?: restProbability
}

internal fun PolymarketLiveQuote?.midpoint(): BigDecimal? {
    if (this?.status != PolymarketLiveQuote.Status.LIVE) return null
    val bid = bestBid ?: return null
    val ask = bestAsk ?: return null
    return if (bid <= ask) (bid + ask).divide(BigDecimal(2)) else null
}

private val MAX_MIDPOINT_SPREAD = BigDecimal("0.10")