package com.tangem.data.feed.search.converter

import com.tangem.data.feed.search.model.ItemDTO
import com.tangem.data.feed.search.model.ItemKind
import com.tangem.data.feed.search.model.MarketTokenDTO
import com.tangem.domain.feed.search.model.RecentFeedSearchItem
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.currency.CryptoCurrency

/**
 * `null` for anything this version cannot render: an unrecognised [ItemDTO.kind], a missing payload,
 * or a price that no longer parses. Dropping the single entry keeps the rest of the history — and a
 * stored price that cannot be read is not worth showing as a real number.
 */
internal fun ItemDTO.toDomain(): RecentFeedSearchItem? = when (kind) {
    ItemKind.MARKET_TOKEN -> marketToken?.toDomain(id = id)
    else -> null
}

internal fun RecentFeedSearchItem.toDTO(timestamp: Long): ItemDTO = when (this) {
    is RecentFeedSearchItem.MarketToken -> ItemDTO(
        kind = ItemKind.MARKET_TOKEN,
        id = token.id.value,
        timestamp = timestamp,
        marketToken = MarketTokenDTO(
            name = token.name,
            symbol = token.symbol,
            imageUrl = token.imageUrl,
            currentPrice = token.tokenQuotes.currentPrice.toPlainString(),
            h24Percent = token.tokenQuotes.h24Percent?.toPlainString(),
            weekPercent = token.tokenQuotes.weekPercent?.toPlainString(),
            monthPercent = token.tokenQuotes.monthPercent?.toPlainString(),
        ),
    )
}

private fun MarketTokenDTO.toDomain(id: String): RecentFeedSearchItem.MarketToken? {
    val price = currentPrice.toBigDecimalOrNull() ?: return null

    return RecentFeedSearchItem.MarketToken(
        token = TokenMarketParams(
            id = CryptoCurrency.RawID(id),
            name = name,
            symbol = symbol,
            tokenQuotes = TokenMarketParams.Quotes(
                currentPrice = price,
                h24Percent = h24Percent?.toBigDecimalOrNull(),
                weekPercent = weekPercent?.toBigDecimalOrNull(),
                monthPercent = monthPercent?.toBigDecimalOrNull(),
            ),
            imageUrl = imageUrl,
        ),
    )
}