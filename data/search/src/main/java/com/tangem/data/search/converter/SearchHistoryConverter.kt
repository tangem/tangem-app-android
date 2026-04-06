package com.tangem.data.search.converter

import com.tangem.data.search.model.RecentTokenDTO
import com.tangem.data.search.model.TextHintDTO
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.search.model.RecentSearchToken
import com.tangem.domain.search.model.SearchTextHint
import com.tangem.domain.search.model.TokenPriceChangeDirection
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class TextHintDTOToSearchTextHintConverter : Converter<TextHintDTO, SearchTextHint> {
    override fun convert(value: TextHintDTO): SearchTextHint {
        return SearchTextHint(
            text = value.text,
            timestamp = value.timestamp,
        )
    }
}

internal class RecentTokenDTOToRecentSearchTokenConverter : Converter<RecentTokenDTO, RecentSearchToken> {
    override fun convert(value: RecentTokenDTO): RecentSearchToken {
        return RecentSearchToken(
            id = CryptoCurrency.RawID(value.id),
            name = value.name,
            symbol = value.symbol,
            imageUrl = value.imageUrl,
            timestamp = value.timestamp,
            appCurrencyCode = value.appCurrencyCode,
            appCurrencySymbol = value.appCurrencySymbol,
            price = value.pricePlain.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            priceChangePercent = value.priceChangePercent,
            priceChangeDirection = value.priceChangeDirection.toTokenPriceChangeDirection(),
            marketCap = value.marketCap,
            ratingPosition = value.ratingPosition,
            isUnder100kMarketCap = value.isUnder100kMarketCap,
            stakingRateText = value.stakingRateText,
        )
    }
}

internal class RecentSearchTokenToRecentTokenDTOConverter : Converter<RecentSearchToken, RecentTokenDTO> {
    override fun convert(value: RecentSearchToken): RecentTokenDTO {
        return RecentTokenDTO(
            id = value.id.value,
            name = value.name,
            symbol = value.symbol,
            imageUrl = value.imageUrl,
            timestamp = value.timestamp,
            pricePlain = value.price.toPlainString(),
            appCurrencyCode = value.appCurrencyCode,
            appCurrencySymbol = value.appCurrencySymbol,
            priceChangePercent = value.priceChangePercent,
            priceChangeDirection = value.priceChangeDirection.name,
            marketCap = value.marketCap,
            ratingPosition = value.ratingPosition,
            isUnder100kMarketCap = value.isUnder100kMarketCap,
            stakingRateText = value.stakingRateText,
        )
    }
}

private fun String.toTokenPriceChangeDirection(): TokenPriceChangeDirection {
    return runCatching { TokenPriceChangeDirection.valueOf(this) }
        .getOrDefault(TokenPriceChangeDirection.NEUTRAL)
}