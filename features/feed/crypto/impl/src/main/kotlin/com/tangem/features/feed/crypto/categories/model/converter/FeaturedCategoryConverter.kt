package com.tangem.features.feed.crypto.categories.model.converter

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.markets.MarketCategory
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.ui.state.FeaturedCategoriesUM
import com.tangem.features.feed.crypto.categories.ui.state.FeaturedCategoryUM
import com.tangem.features.feed.crypto.impl.R
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

/**
 * Builds the featured badge rail.
 */
internal class FeaturedCategoryConverter(
    private val selection: MarketCategorySelection,
    private val onSelectionChange: (MarketCategorySelection) -> Unit,
) : Converter<List<MarketCategory>, FeaturedCategoriesUM> {

    override fun convert(value: List<MarketCategory>): FeaturedCategoriesUM {
        return FeaturedCategoriesUM(
            title = resourceReference(R.string.common_featured),
            items = value.map { category ->
                val isSelected = selection.category?.id == category.id

                FeaturedCategoryUM(
                    id = category.id,
                    label = stringReference(category.name),
                    isSelected = isSelected,
                    onItemClick = {
                        onSelectionChange(
                            if (isSelected) {
                                MarketCategorySelection()
                            } else {
                                MarketCategorySelection(
                                    category = MarketCategorySelection.Entry(
                                        id = category.id,
                                        name = category.name,
                                    ),
                                )
                            },
                        )
                    },
                )
            }.toImmutableList(),
        )
    }
}