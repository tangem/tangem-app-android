package com.tangem.features.feed.model.search.converter

import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.search.model.RecentSearchToken
import com.tangem.domain.search.model.TokenPriceChangeDirection
import com.tangem.utils.converter.Converter

internal class MarketsListItemUMToRecentSearchTokenConverter :
    Converter<MarketsListItemUMWithAppCurrency, RecentSearchToken> {

    override fun convert(value: MarketsListItemUMWithAppCurrency): RecentSearchToken {
        val item = value.item
        val direction = when (item.trendType) {
            PriceChangeType.UP -> TokenPriceChangeDirection.UP
            PriceChangeType.DOWN -> TokenPriceChangeDirection.DOWN
            PriceChangeType.NEUTRAL -> TokenPriceChangeDirection.NEUTRAL
        }
        val stakingText = when (val rate = item.stakingRate) {
            is TextReference.Str -> rate.value
            is TextReference.StyledStr -> rate.value
            else -> null
        }
        return RecentSearchToken(
            id = item.id,
            name = item.name,
            symbol = item.currencySymbol,
            imageUrl = item.iconUrl,
            timestamp = item.updateTimestamp ?: System.currentTimeMillis(),
            appCurrencyCode = value.appCurrencyCode,
            appCurrencySymbol = value.appCurrencySymbol,
            price = item.price.fiatPrice,
            priceChangePercent = item.trendPercentText,
            priceChangeDirection = direction,
            marketCap = item.marketCap,
            ratingPosition = item.ratingPosition,
            isUnder100kMarketCap = item.isUnder100kMarketCap,
            stakingRateText = stakingText,
        )
    }
}