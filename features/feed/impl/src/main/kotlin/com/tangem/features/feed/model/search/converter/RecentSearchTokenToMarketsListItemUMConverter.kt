package com.tangem.features.feed.model.search.converter

import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.common.ui.markets.toMarketsListItemPriceAnnotated
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.price
import com.tangem.domain.search.model.TokenPriceChangeDirection
import com.tangem.utils.converter.Converter

internal class RecentSearchTokenToMarketsListItemUMConverter :
    Converter<RecentSearchTokenWithAppCurrency, MarketsListItemUM> {

    override fun convert(value: RecentSearchTokenWithAppCurrency): MarketsListItemUM {
        val token = value.token
        val currencyCode = token.appCurrencyCode.ifEmpty { value.appCurrency.code }
        val currencySymbol = token.appCurrencySymbol.ifEmpty { value.appCurrency.symbol }
        val trendType = when (token.priceChangeDirection) {
            TokenPriceChangeDirection.UP -> PriceChangeType.UP
            TokenPriceChangeDirection.DOWN -> PriceChangeType.DOWN
            TokenPriceChangeDirection.NEUTRAL -> PriceChangeType.NEUTRAL
        }
        val priceText = token.price.format {
            fiat(
                fiatCurrencyCode = currencyCode,
                fiatCurrencySymbol = currencySymbol,
            ).price()
        }
        return MarketsListItemUM(
            id = token.id,
            name = token.name,
            currencySymbol = token.symbol,
            iconUrl = token.imageUrl,
            ratingPosition = token.ratingPosition,
            marketCap = token.marketCap,
            price = MarketsListItemUM.Price(
                text = priceText,
                annotated = token.price.toMarketsListItemPriceAnnotated(
                    appCurrencyCode = currencyCode,
                    appCurrencySymbol = currencySymbol,
                ),
                changeType = null,
                fiatPrice = token.price,
            ),
            trendPercentText = token.priceChangePercent,
            trendType = trendType,
            chartData = null,
            isUnder100kMarketCap = token.isUnder100kMarketCap,
            stakingRate = token.stakingRateText?.let { TextReference.Str(it) },
            updateTimestamp = token.timestamp,
        )
    }
}