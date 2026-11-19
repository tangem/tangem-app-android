package com.tangem.features.feed.rwa.categories.model.converter

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.markets.CoinCategory
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.ReservedCategoryCodes
import com.tangem.features.feed.rwa.categories.RwaCategoryNames
import com.tangem.features.feed.rwa.categories.ui.state.CategoryItemUM
import com.tangem.features.feed.rwa.categories.ui.state.LegacyCategorySelectorSectionUM
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Builds the "Type" rows of the Category selector — the draft/Apply flow from before the redesign.
 *
 * The `real-world-assets` umbrella is not a plain filter: it is the tab's default scope, so it renders
 * as "All" and stands for *no* category filter. Picking it therefore emits the empty
 * [CategorySelection], and it checks itself when nothing is applied — the umbrella id never travels in
 * a selection.
 *
 * Only *moving* to another category resets the sector; tapping the row that is already selected
 * republishes the selection untouched, so the sector picked in its rail survives the tap.
 */
@RemoveWithToggle("AND_19514_CATEGORY_FILTERS_REDESIGN_ENABLED")
internal class LegacyCategoryTagConverter(
    private val categorySelection: CategorySelection,
    private val onSelectionChange: (CategorySelection) -> Unit,
) : Converter<List<CoinCategory>, LegacyCategorySelectorSectionUM.Items> {

    override fun convert(value: List<CoinCategory>): LegacyCategorySelectorSectionUM.Items {
        return LegacyCategorySelectorSectionUM.Items(
            title = resourceReference(R.string.common_type),
            items = value.map { category ->
                val isUmbrella = category.code == ReservedCategoryCodes.REAL_WORLD_ASSETS
                val isSelected = if (isUmbrella) {
                    categorySelection.category == null
                } else {
                    categorySelection.category?.id == category.id
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
                    sectors = persistentListOf(),
                    onItemClick = {
                        onSelectionChange(
                            when {
                                isUmbrella -> CategorySelection()
                                isSelected -> categorySelection
                                else -> CategorySelection(
                                    category = CategorySelection.Entry(
                                        id = category.id,
                                        displayName = category.displayName,
                                        code = category.code,
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