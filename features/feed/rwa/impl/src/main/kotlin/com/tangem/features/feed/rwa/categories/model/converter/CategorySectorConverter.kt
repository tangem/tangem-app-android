package com.tangem.features.feed.rwa.categories.model.converter

import com.tangem.domain.markets.CoinCategory
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.RwaCategoryNames
import com.tangem.features.feed.rwa.categories.model.toSelectionEntry
import com.tangem.features.feed.rwa.categories.ui.state.CategorySectorUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Builds the sector chips of a category.
 *
 * There is no "All" chip: no chip selected means the whole category, so tapping the selected chip
 * toggles it off and hands back the category alone.
 */
internal class CategorySectorConverter(
    private val categorySelection: CategorySelection,
    private val onSelectionChange: (CategorySelection) -> Unit,
) : Converter<CoinCategory, ImmutableList<CategorySectorUM>> {

    override fun convert(value: CoinCategory): ImmutableList<CategorySectorUM> {
        val category = value.toSelectionEntry()
        val isCategoryApplied = categorySelection.category?.id == value.id

        return value.sectors.map { sector ->
            val isSelected = isCategoryApplied && categorySelection.sector?.id == sector.id

            CategorySectorUM(
                id = sector.id,
                label = RwaCategoryNames.sector(
                    categoryCode = value.code,
                    sectorCode = sector.code,
                    fallback = sector.displayName,
                ),
                isSelected = isSelected,
                onItemClick = {
                    onSelectionChange(
                        CategorySelection(
                            category = category,
                            sector = if (isSelected) {
                                null
                            } else {
                                CategorySelection.Entry(
                                    id = sector.id,
                                    displayName = sector.displayName,
                                    code = sector.code,
                                )
                            },
                        ),
                    )
                },
            )
        }.toImmutableList()
    }
}