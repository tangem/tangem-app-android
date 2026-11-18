package com.tangem.features.feed.rwa.categories.model.converter

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.markets.CoinCategory
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.ReservedCategoryCodes
import com.tangem.features.feed.rwa.categories.RwaCategoryNames
import com.tangem.features.feed.rwa.categories.model.hasSectorChoice
import com.tangem.features.feed.rwa.categories.model.toSelectionEntry
import com.tangem.features.feed.rwa.categories.ui.state.CategoryItemUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Builds the category rows of the Category selector; every row with a sector choice carries its chips.
 *
 * The `real-world-assets` umbrella is not a plain filter: it is the tab's default scope, so it renders
 * as "All" and stands for *no* category filter. Picking it therefore emits the empty
 * [CategorySelection], and it checks itself when nothing is applied — the umbrella id never travels in
 * a selection.
 *
 * A row is checked only for its whole category: once one of its sectors is picked, the chip carries the
 * check instead, and tapping the row drops the sector.
 */
internal class CategoryTagConverter(
    private val categorySelection: CategorySelection,
    private val onSelectionChange: (CategorySelection) -> Unit,
) : Converter<List<CoinCategory>, ImmutableList<CategoryItemUM>> {

    private val sectorConverter = CategorySectorConverter(
        categorySelection = categorySelection,
        onSelectionChange = onSelectionChange,
    )

    override fun convert(value: List<CoinCategory>): ImmutableList<CategoryItemUM> {
        return value.map { category ->
            val isUmbrella = category.code == ReservedCategoryCodes.REAL_WORLD_ASSETS
            val isSelected = if (isUmbrella) {
                categorySelection.category == null
            } else {
                categorySelection.category?.id == category.id && categorySelection.sector == null
            }

            CategoryItemUM(
                id = category.id,
                title = if (isUmbrella) {
                    resourceReference(R.string.common_all)
                } else {
                    RwaCategoryNames.category(code = category.code, fallback = category.displayName)
                },
                caption = pluralReference(
                    id = R.plurals.common_assets_count,
                    count = category.tokensCount,
                    formatArgs = wrappedList(category.tokensCount),
                ),
                isSelected = isSelected,
                sectors = if (category.hasSectorChoice) {
                    sectorConverter.convert(category)
                } else {
                    persistentListOf()
                },
                onItemClick = {
                    onSelectionChange(
                        if (isUmbrella) {
                            CategorySelection()
                        } else {
                            CategorySelection(category = category.toSelectionEntry())
                        },
                    )
                },
            )
        }.toImmutableList()
    }
}