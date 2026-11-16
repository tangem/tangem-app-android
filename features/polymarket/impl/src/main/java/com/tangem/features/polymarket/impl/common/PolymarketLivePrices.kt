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

internal fun PolymarketLiveQuote?.probability(restProbability: BigDecimal?): BigDecimal? {
    if (this?.status != PolymarketLiveQuote.Status.LIVE) return restProbability
    val bid = bestBid
    val ask = bestAsk
    return if (bid != null && ask != null && ask - bid <= MAX_MIDPOINT_SPREAD) {
        (bid + ask).divide(BigDecimal(2))
    } else {
        lastTradePrice ?: restProbability
    }
}

private val MAX_MIDPOINT_SPREAD = BigDecimal("0.10")