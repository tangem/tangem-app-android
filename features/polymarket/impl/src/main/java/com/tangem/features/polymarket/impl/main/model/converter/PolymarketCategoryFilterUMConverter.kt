package com.tangem.features.polymarket.impl.main.model.converter

import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketCategoryFilter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal object PolymarketCategoryFilterUMConverter {

    private const val MINIMUM_FILTER_COUNT = 2

    fun available(category: PolymarketCategory?): List<PolymarketCategoryFilter> = category?.filters.orEmpty()
        .filterNot { it is PolymarketCategoryFilter.Unknown }
        .takeIf { it.size >= MINIMUM_FILTER_COUNT }.orEmpty()

    fun convert(
        category: PolymarketCategory?,
        selected: PolymarketCategoryFilter?,
        onSelect: (PolymarketCategoryFilter) -> Unit,
    ): ImmutableList<TangemTabItemUM.Content> = available(category).map { filter ->
        TangemTabItemUM.Content(
            id = filter.value,
            // Matches the category screen until the shared Lokalise keys are available.
            label = stringReference(if (filter == PolymarketCategoryFilter.Games) "Games" else "Questions"),
            isSelected = filter == selected,
            onClick = { onSelect(filter) },
        )
    }.toImmutableList()
}