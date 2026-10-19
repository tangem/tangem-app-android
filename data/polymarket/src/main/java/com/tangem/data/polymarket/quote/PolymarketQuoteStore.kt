package com.tangem.data.polymarket.quote

import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class PolymarketQuoteStore(private val timeSource: TimeSource) {

    private val entries = mutableMapOf<String, Entry>()
    private val lock = Any()

    fun apply(updates: List<PolymarketQuoteUpdate>) = synchronized(lock) {
        updates.forEach { update ->
            val entry = entries.getOrPut(update.assetId) { Entry(timeSource.markNow()) }
            var bookUpdated = false
            if (update.bid != null && update.timestamp >= entry.bidTime) {
                entry.bid = update.bid
                entry.bidTime = update.timestamp
                bookUpdated = true
            }
            if (update.ask != null && update.timestamp >= entry.askTime) {
                entry.ask = update.ask
                entry.askTime = update.timestamp
                bookUpdated = true
            }
            if (update.lastTradePrice != null && update.timestamp >= entry.tradeTime) {
                entry.tradePrice = update.lastTradePrice
                entry.tradeTime = update.timestamp
            }
            if (bookUpdated) {
                entry.lastBookSeen = timeSource.markNow()
                entry.refreshRequested = null
            }
        }
    }

    fun snapshot(assetIds: Set<String>, isConnected: Boolean): Snapshot = synchronized(lock) {
        entries.entries.removeAll { (id, entry) ->
            id !in assetIds && entry.lastObserved.elapsedNow() > CACHE_RETENTION
        }
        val refresh = mutableSetOf<String>()
        val quotes = assetIds.associateWith { assetId ->
            val entry = entries.getOrPut(assetId) { Entry(timeSource.markNow()) }
            entry.lastObserved = timeSource.markNow()
            if (isConnected && entry.lastBookSeen.elapsedNow() > STALE_AFTER && entry.refreshRequested == null) {
                entry.refreshRequested = timeSource.markNow()
                refresh += assetId
            }
            val isComplete = entry.bid != null && entry.ask != null
            val isStale = !isConnected || entry.refreshRequested?.elapsedNow()?.let { it >= REFRESH_GRACE } == true
            PolymarketLiveQuote(
                assetId = assetId,
                bestBid = entry.bid?.price,
                bestAsk = entry.ask?.price,
                lastTradePrice = entry.tradePrice,
                updatedAtMillis = maxOf(entry.bidTime, entry.askTime).takeIf { it >= 0 },
                status = when {
                    !isComplete -> PolymarketLiveQuote.Status.WAITING
                    isStale -> PolymarketLiveQuote.Status.STALE
                    else -> PolymarketLiveQuote.Status.LIVE
                },
            )
        }
        Snapshot(quotes, refresh)
    }

    data class Snapshot(val quotes: Map<String, PolymarketLiveQuote>, val resubscribe: Set<String>)

    private class Entry(var lastBookSeen: TimeMark) {
        var lastObserved: TimeMark = lastBookSeen
        var bid: PolymarketQuoteUpdate.Side? = null
        var ask: PolymarketQuoteUpdate.Side? = null
        var bidTime: Long = -1
        var askTime: Long = -1
        var tradePrice: java.math.BigDecimal? = null
        var tradeTime: Long = -1
        var refreshRequested: TimeMark? = null
    }

    private companion object {
        val STALE_AFTER = 60.seconds
        val REFRESH_GRACE = 5.seconds
        val CACHE_RETENTION = 5.minutes
    }
}