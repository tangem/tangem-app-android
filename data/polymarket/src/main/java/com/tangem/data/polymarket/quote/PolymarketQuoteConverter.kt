package com.tangem.data.polymarket.quote

import com.tangem.grow.datasource.polymarket.socket.PolymarketMarketEventDto
import java.math.BigDecimal

internal object PolymarketQuoteConverter {

    fun convert(event: PolymarketMarketEventDto): List<PolymarketQuoteUpdate> = when (event) {
        is PolymarketMarketEventDto.Book -> listOfNotNull(book(event))
        is PolymarketMarketEventDto.PriceChange -> event.priceChanges.mapNotNull { change ->
            prices(assetId = change.assetId, bid = change.bestBid, ask = change.bestAsk, timestamp = event.timestamp)
        }
        is PolymarketMarketEventDto.BestBidAsk ->
            listOfNotNull(
                prices(assetId = event.assetId, bid = event.bestBid, ask = event.bestAsk, timestamp = event.timestamp),
            )
        is PolymarketMarketEventDto.LastTradePrice -> listOfNotNull(trade(event))
        is PolymarketMarketEventDto.Unknown -> emptyList()
    }

    private fun book(event: PolymarketMarketEventDto.Book): PolymarketQuoteUpdate? {
        val timestamp = timestamp(event.timestamp) ?: return null
        val bids = levels(event.bids) ?: return null
        val asks = levels(event.asks) ?: return null
        return PolymarketQuoteUpdate(
            assetId = event.assetId,
            timestamp = timestamp,
            bid = bid(bids.maxOrNull()),
            ask = ask(asks.minOrNull()),
        )
    }

    private fun prices(assetId: String, bid: String?, ask: String?, timestamp: String): PolymarketQuoteUpdate? {
        val time = timestamp(timestamp) ?: return null
        val bestBid = bid?.let { price(it) ?: return null }
        val bestAsk = ask?.let { price(it) ?: return null }
        if (bid == null && ask == null) return null
        return PolymarketQuoteUpdate(
            assetId = assetId,
            timestamp = time,
            bid = if (bid != null) bid(bestBid) else null,
            ask = if (ask != null) ask(bestAsk) else null,
        )
    }

    private fun trade(event: PolymarketMarketEventDto.LastTradePrice): PolymarketQuoteUpdate? {
        val timestamp = timestamp(event.timestamp) ?: return null
        val price = price(event.price) ?: return null
        return PolymarketQuoteUpdate(event.assetId, timestamp, lastTradePrice = price)
    }

    private fun levels(levels: List<PolymarketMarketEventDto.Level>): List<BigDecimal>? = buildList {
        levels.forEach { level ->
            val price = price(level.price) ?: return null
            val size = level.size.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 } ?: return null
            if (size.signum() > 0) add(price)
        }
    }

    private fun price(raw: String): BigDecimal? = raw.toBigDecimalOrNull()
        ?.takeIf { it >= BigDecimal.ZERO && it <= BigDecimal.ONE }
        ?.stripTrailingZeros()

    private fun timestamp(raw: String): Long? = raw.toLongOrNull()?.takeIf { it >= 0 }

    private fun bid(price: BigDecimal?) = PolymarketQuoteUpdate.Side(price?.takeUnless { it.signum() == 0 })

    private fun ask(price: BigDecimal?) = PolymarketQuoteUpdate.Side(
        price?.takeUnless { it.compareTo(BigDecimal.ONE) == 0 },
    )
}