package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/**
 * The outcome a crypto window ended on.
 *
 * @property isResolved whether the backend has settled the market on [outcome]; until then the prices alone tell it
 */
data class PolymarketCryptoWinner(
    val outcome: PolymarketOutcome,
    val isResolved: Boolean,
)

/**
 * The winner of a crypto Up/Down event: the outcome the backend has settled the market on, and before that the one
 * the confirmed prices point at, which is Up unless the final price is below the price to beat. The coin's live
 * price never names a winner. Absent while neither is known and for an event that is not Up/Down.
 *
 * The clock is not consulted: the answer means something for a window that has ended only.
 */
fun PolymarketEvent.cryptoWinner(): PolymarketCryptoWinner? {
    val window = cryptoWindow ?: return null
    val outcomes = markets.firstOrNull()?.outcomes.orEmpty()
    val resolved = outcomes.find { it.probability?.compareTo(BigDecimal.ONE) == 0 }
    if (resolved != null) return PolymarketCryptoWinner(outcome = resolved, isResolved = true)

    val priceToBeat = window.priceToBeat ?: return null
    val finalPrice = window.finalPrice ?: return null
    val isUp = finalPrice >= priceToBeat
    val byPrices = outcomes.find { it.title.equals(if (isUp) UP_TITLE else DOWN_TITLE, ignoreCase = true) }
        ?: outcomes.getOrNull(if (isUp) UP_INDEX else DOWN_INDEX)

    return byPrices?.let { PolymarketCryptoWinner(outcome = it, isResolved = false) }
}

private const val UP_TITLE = "Up"
private const val DOWN_TITLE = "Down"
private const val UP_INDEX = 0
private const val DOWN_INDEX = 1