package com.tangem.data.markets.converters

import com.tangem.store.datasource.markets.models.response.CoinCategoriesResponse
import com.tangem.domain.markets.CoinCategory
import com.tangem.utils.converter.Converter

/**
 * Converter from [CoinCategoriesResponse.Category] to [CoinCategory]
 *
[REDACTED_AUTHOR]
 */
internal object CoinCategoryConverter : Converter<CoinCategoriesResponse.Category, CoinCategory> {

    override fun convert(value: CoinCategoriesResponse.Category): CoinCategory {
        return CoinCategory(
            id = value.id,
            code = value.code,
            displayName = value.displayName,
            displayOrder = value.displayOrder,
            tokensCount = value.tokensCount,
            isRestricted = value.isRestricted,
            isVisible = value.isVisible,
            sectors = value.sectors.map(::convertSector),
        )
    }

    private fun convertSector(value: CoinCategoriesResponse.Category.Sector): CoinCategory.Sector {
        return CoinCategory.Sector(
            id = value.id,
            displayName = value.displayName,
            tokensCount = value.tokensCount,
        )
    }
}