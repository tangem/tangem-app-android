package com.tangem.features.collectibles.impl.main.model.converter

import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.main.model.CollectiblesCategory
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Builds the category band with the first category selected; every later selection is a state update. */
internal class CollectiblesTabsConverter(
    private val onCategoryClick: (String) -> Unit,
) : Converter<List<CollectiblesCategory>, ImmutableList<TangemTabItemUM>> {

    override fun convert(value: List<CollectiblesCategory>): ImmutableList<TangemTabItemUM> {
        return value
            .mapIndexed { index, category ->
                TangemTabItemUM.Content(
                    id = category.id,
                    label = stringReference(category.label),
                    counter = stringReference(category.count.toString()),
                    isSelected = index == 0,
                    onClick = { onCategoryClick(category.id) },
                )
            }
            .toImmutableList()
    }
}