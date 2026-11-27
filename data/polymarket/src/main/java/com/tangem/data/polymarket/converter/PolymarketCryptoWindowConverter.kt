package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketCryptoNextWindow
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceFeed
import com.tangem.domain.polymarket.model.PolymarketCryptoWindow
import com.tangem.grow.datasource.polymarket.models.PolymarketCryptoNextEventDto
import com.tangem.grow.datasource.polymarket.models.PolymarketCryptoPriceFeedDto
import com.tangem.grow.datasource.polymarket.models.PolymarketEventDto
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.time.Instant

/**
 * Reads the window of a crypto Up/Down event. An event whose block is absent or names no valid window converts to
 * `null` and stays an ordinary event.
 */
internal object PolymarketCryptoWindowConverter : Converter<PolymarketEventDto, PolymarketCryptoWindow?> {

    override fun convert(value: PolymarketEventDto): PolymarketCryptoWindow? {
        val block = value.cryptoUpDown ?: return null
        val start = block.windowStart?.toInstantOrNull() ?: return null
        val end = value.endDate?.toInstantOrNull() ?: return null
        if (!end.isAfter(start)) return null

        return PolymarketCryptoWindow(
            start = start,
            end = end,
            priceFeed = block.priceFeed?.let(::convertPriceFeed),
            priceToBeat = block.priceToBeat?.let(BigDecimal::valueOf),
            finalPrice = block.finalPrice?.let(BigDecimal::valueOf),
            nextWindows = block.nextEvents.orEmpty().mapNotNull(::convertNextWindow),
            liveEventId = block.liveEventId?.takeIf(String::isNotBlank),
        )
    }

    private fun convertPriceFeed(feed: PolymarketCryptoPriceFeedDto): PolymarketCryptoPriceFeed? {
        val channel = feed.channel?.takeIf(String::isNotBlank) ?: return null
        val filter = feed.filter ?: return null
        val symbol = filter.symbol?.takeIf(String::isNotBlank) ?: return null

        return PolymarketCryptoPriceFeed(
            channel = channel,
            symbol = symbol,
            provider = filter.provider?.takeIf(String::isNotBlank),
            windowSeconds = filter.windowSeconds,
        )
    }

    private fun convertNextWindow(next: PolymarketCryptoNextEventDto): PolymarketCryptoNextWindow? {
        val eventId = next.id?.takeIf(String::isNotBlank) ?: return null
        val start = next.windowStart?.toInstantOrNull() ?: return null
        val end = next.windowEnd?.toInstantOrNull() ?: return null
        if (!end.isAfter(start)) return null

        return PolymarketCryptoNextWindow(eventId = eventId, start = start, end = end)
    }

    private fun String.toInstantOrNull(): Instant? = runCatching { Instant.parse(this) }.getOrNull()
}