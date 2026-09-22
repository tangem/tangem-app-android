package com.tangem.features.collectibles.impl.discovery.model.converter

import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.collectibles.models.CollectibleGachaMachineCategories
import com.tangem.features.collectibles.impl.R
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Builds the category band: a synthesized "All cards" tab first, then one tab per backend category.
 * The "All" tab reports `null` to [onCategoryClick] — an unfiltered catalogue request.
 */
internal class CollectiblesDiscoveryTabsConverter(
    private val onCategoryClick: (String?) -> Unit,
) : Converter<CollectiblesDiscoveryTabsConverter.Input, ImmutableList<TangemTabItemUM>> {

    data class Input(
        val categories: CollectibleGachaMachineCategories,
        val selectedCategoryId: String?,
    )

    override fun convert(value: Input): ImmutableList<TangemTabItemUM> {
        val allTab = TangemTabItemUM.Content(
            id = ALL_TAB_ID,
            label = TextReference.Res(R.string.collectibles_main_all_packs_tab),
            counter = stringReference(value.categories.totalMachinesQuantity.toString()),
            isSelected = value.selectedCategoryId == null,
            onClick = { onCategoryClick(null) },
        )
        val categoryTabs = value.categories.categories.map { category ->
            TangemTabItemUM.Content(
                id = category.id,
                label = stringReference(category.name),
                counter = stringReference(category.machinesQuantity.toString()),
                isSelected = category.id == value.selectedCategoryId,
                onClick = { onCategoryClick(category.id) },
            )
        }
        return (listOf(allTab) + categoryTabs).toImmutableList()
    }

    private companion object {

        const val ALL_TAB_ID = "all"
    }
}