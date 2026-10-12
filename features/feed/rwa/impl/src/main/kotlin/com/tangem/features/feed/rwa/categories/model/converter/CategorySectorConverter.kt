package com.tangem.features.feed.rwa.categories.model.converter

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.markets.CoinCategory
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.RwaCategoryNames
import com.tangem.features.feed.rwa.categories.ui.state.CategorySectorUM
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorSectionUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

/**
 * Builds the "Sectors" rail of the selected category.
 *
 * "All" here narrows nothing *within* the category — it leaves the category itself alone, the same way
 * the umbrella row means "no category filter" without clearing anything above it.
 */
internal class CategorySectorConverter(
    private val categorySelection: CategorySelection,
    private val onSelectionChange: (CategorySelection) -> Unit,
) : Converter<CoinCategory, CategorySelectorSectionUM.Sectors> {

    override fun convert(value: CoinCategory): CategorySelectorSectionUM.Sectors {
        val allTag = CategorySectorUM(
            id = ALL_SECTORS_ID,
            label = resourceReference(R.string.common_all),
            isSelected = categorySelection.sector == null,
            onItemClick = { onSelectionChange(CategorySelection(category = categorySelection.category)) },
        )
        val sectorTags = value.sectors.map { sector ->
            CategorySectorUM(
                id = sector.id,
                label = RwaCategoryNames.sector(
                    categoryCode = value.code,
                    sectorCode = sector.code,
                    fallback = sector.displayName,
                ),
                isSelected = categorySelection.sector?.id == sector.id,
                onItemClick = {
                    onSelectionChange(
                        CategorySelection(
                            category = categorySelection.category,
                            sector = CategorySelection.Entry(
                                id = sector.id,
                                displayName = sector.displayName,
                                code = sector.code,
                            ),
                        ),
                    )
                },
            )
        }

        return CategorySelectorSectionUM.Sectors(
            title = resourceReference(R.string.markets_real_assets_filter_sectors_title),
            sectors = (listOf(allTag) + sectorTags).toImmutableList(),
        )
    }

    private companion object {
        const val ALL_SECTORS_ID = "__all__"
    }
}