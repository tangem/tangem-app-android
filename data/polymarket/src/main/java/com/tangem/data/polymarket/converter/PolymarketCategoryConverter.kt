package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketCategoryFilter
import com.tangem.domain.polymarket.model.PolymarketCategoryIcon
import com.tangem.grow.datasource.polymarket.models.PolymarketCategoryDto
import com.tangem.utils.converter.Converter

internal object PolymarketCategoryConverter : Converter<PolymarketCategoryDto, PolymarketCategory> {

    override fun convert(value: PolymarketCategoryDto): PolymarketCategory {
        val seenIds = mutableSetOf(value.id)
        return PolymarketCategory(
            id = value.id,
            label = value.label,
            icon = PolymarketCategoryIcon.fromKey(value.icon),
            subcategories = value.subcategories.orEmpty()
                .filter { seenIds.add(it.id) }
                .map(::convert),
            filters = value.filters.orEmpty().map(PolymarketCategoryFilter::fromValue),
        )
    }
}