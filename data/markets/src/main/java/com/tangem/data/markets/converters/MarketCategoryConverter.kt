package com.tangem.data.markets.converters

import com.tangem.domain.markets.MarketCategory
import com.tangem.domain.markets.MarketCategoryPage
import com.tangem.store.datasource.markets.models.response.MarketCategoriesResponse
import com.tangem.utils.converter.Converter

/**
 * Converter from [MarketCategoriesResponse] to [MarketCategoryPage]
 *
[REDACTED_AUTHOR]
 */
internal object MarketCategoryConverter : Converter<MarketCategoriesResponse, MarketCategoryPage> {

    override fun convert(value: MarketCategoriesResponse): MarketCategoryPage {
        return MarketCategoryPage(
            categories = value.categories.map(::convertCategory),
            total = value.total,
            offset = value.offset,
            limit = value.limit,
        )
    }

    private fun convertCategory(value: MarketCategoriesResponse.Category): MarketCategory {
        return MarketCategory(
            id = value.id,
            name = value.name,
            description = value.description?.takeIf(String::isNotBlank),
            marketCap = value.marketCap,
            marketCapChangePercent24h = value.marketCapChange24h?.movePointLeft(2),
            volume24h = value.volume24h,
            isFeatured = value.isFeatured,
            tokensCount = value.tokensCount,
        )
    }
}