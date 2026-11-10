package com.tangem.features.polymarket.impl.common.feed.model.converter

import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketCategoryFilter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal object PolymarketCategoryFilterUMConverter {

    fun convert(
        category: PolymarketCategory?,
        selected: PolymarketCategoryFilter?,
        onSelect: (PolymarketCategoryFilter) -> Unit,
    ): ImmutableList<TangemTabItemUM.Content> = category?.selectableFilters.orEmpty().mapNotNull { filter ->
        val label = filter.label() ?: return@mapNotNull null
        TangemTabItemUM.Content(
            id = filter.value,
            label = label,
            isSelected = filter == selected,
            onClick = { onSelect(filter) },
        )
    }.toImmutableList()

    private fun PolymarketCategoryFilter.label(): TextReference? = when (this) {
        PolymarketCategoryFilter.Games -> stringReference("Games")
        PolymarketCategoryFilter.Props -> stringReference("Questions")
        is PolymarketCategoryFilter.Unknown -> null
    }
}