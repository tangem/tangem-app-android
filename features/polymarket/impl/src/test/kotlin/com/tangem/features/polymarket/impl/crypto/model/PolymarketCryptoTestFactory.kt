package com.tangem.features.polymarket.impl.crypto.model

import com.tangem.domain.polymarket.model.PolymarketCryptoNextWindow
import com.tangem.domain.polymarket.model.PolymarketCryptoWindow
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.createPolymarketMarket
import com.tangem.features.polymarket.impl.createPolymarketOutcome
import java.math.BigDecimal
import java.time.Instant

internal const val CRYPTO_WINDOW_MILLIS = 300_000L

/**
 * Window `w<index>` of a series of five-minute windows that stand back to back, `w0` starting at zero, with the
 * [following] windows after it named as the backend names them.
 */
internal fun createCryptoEvent(
    index: Int,
    following: Int = 20,
    title: String = "Bitcoin Up or Down $index",
    priceToBeat: BigDecimal? = null,
    finalPrice: BigDecimal? = null,
    liveEventId: String? = null,
    outcomes: List<PolymarketOutcome> = listOf(
        createPolymarketOutcome(assetId = "up-$index", title = "Up"),
        createPolymarketOutcome(assetId = "down-$index", title = "Down"),
    ),
): PolymarketEvent {
    val id = cryptoWindowId(index)

    return createPolymarketEvent(
        id = id,
        title = title,
        markets = listOf(createPolymarketMarket(id = "market-$index", eventId = id, outcomes = outcomes)),
    ).copy(
        cryptoWindow = PolymarketCryptoWindow(
            start = Instant.ofEpochMilli(cryptoWindowStart(index)),
            end = Instant.ofEpochMilli(cryptoWindowStart(index + 1)),
            priceFeed = null,
            priceToBeat = priceToBeat,
            finalPrice = finalPrice,
            nextWindows = (index + 1..index + following).map { next ->
                PolymarketCryptoNextWindow(
                    eventId = cryptoWindowId(next),
                    start = Instant.ofEpochMilli(cryptoWindowStart(next)),
                    end = Instant.ofEpochMilli(cryptoWindowStart(next + 1)),
                )
            },
            liveEventId = liveEventId,
        ),
    )
}

internal fun cryptoWindowId(index: Int): String = "w$index"

internal fun cryptoWindowStart(index: Int): Long = index * CRYPTO_WINDOW_MILLIS

/** A moment a minute into window `w<index>`. */
internal fun duringCryptoWindow(index: Int): Long = cryptoWindowStart(index) + 60_000L