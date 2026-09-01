package com.tangem.features.polymarket.impl.main.model.converter

import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketCategoryTabUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Converts the loaded categories into the tab band of the Discovery feed.
 *
 * @property onCategoryClick called with the id of the tapped category
 */
internal class PolymarketCategoryTabUMConverter(
    private val onCategoryClick: (Int) -> Unit,
) : Converter<PolymarketCategoryTabUMConverter.Input, ImmutableList<PolymarketCategoryTabUM>> {

    override fun convert(value: Input): ImmutableList<PolymarketCategoryTabUM> = value.categories
        .map { category ->
            PolymarketCategoryTabUM(
                id = category.id,
                label = category.label,
                isSelected = category.id == value.selectedCategoryId,
                onClick = { onCategoryClick(category.id) },
            )
        }
        .toImmutableList()

    /**
     * @property categories categories in the order the backend serves them
     * @property selectedCategoryId id of the category the feed is filtered by, `null` when it runs unfiltered
     */
    data class Input(
        val categories: List<PolymarketCategory>,
        val selectedCategoryId: Int?,
    )
}