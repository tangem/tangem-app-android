package com.tangem.data.feed.search.converter

import com.tangem.data.feed.search.model.ItemDTO
import com.tangem.domain.feed.search.model.RecentFeedSearchItem
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class ItemDTOToRecentFeedSearchItemConverter : Converter<ItemDTO, RecentFeedSearchItem> {

    override fun convert(value: ItemDTO): RecentFeedSearchItem = when (value) {
        is ItemDTO.MarketToken -> RecentFeedSearchItem.MarketToken(
            token = TokenMarketParams(
                id = CryptoCurrency.RawID(value.id),
                name = value.name,
                symbol = value.symbol,
                tokenQuotes = TokenMarketParams.Quotes(
                    currentPrice = value.currentPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    h24Percent = value.h24Percent?.toBigDecimalOrNull(),
                    weekPercent = value.weekPercent?.toBigDecimalOrNull(),
                    monthPercent = value.monthPercent?.toBigDecimalOrNull(),
                ),
                imageUrl = value.imageUrl,
            ),
        )
    }
}

internal fun RecentFeedSearchItem.toDTO(timestamp: Long): ItemDTO = when (this) {
    is RecentFeedSearchItem.MarketToken -> ItemDTO.MarketToken(
        id = token.id.value,
        name = token.name,
        symbol = token.symbol,
        imageUrl = token.imageUrl,
        currentPrice = token.tokenQuotes.currentPrice.toPlainString(),
        h24Percent = token.tokenQuotes.h24Percent?.toPlainString(),
        weekPercent = token.tokenQuotes.weekPercent?.toPlainString(),
        monthPercent = token.tokenQuotes.monthPercent?.toPlainString(),
        timestamp = timestamp,
    )
}