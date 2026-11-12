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
    return midpoint() ?: lastTradePrice ?: restProbability
}

internal fun PolymarketLiveQuote?.probabilityUpdatedAtMillis(): Long? {
    if (this?.status != PolymarketLiveQuote.Status.LIVE) return null
    return when {
        midpoint() != null -> updatedAtMillis
        lastTradePrice != null -> listOfNotNull(updatedAtMillis, lastTradeAtMillis).maxOrNull()
        else -> null
    }
}

private fun PolymarketLiveQuote.midpoint(): BigDecimal? {
    val bid = bestBid ?: return null
    val ask = bestAsk ?: return null
    return if (bid <= ask && ask - bid <= MAX_MIDPOINT_SPREAD) {
        (bid + ask).divide(BigDecimal(2))
    } else {
        null
    }
}

private val MAX_MIDPOINT_SPREAD = BigDecimal("0.10")