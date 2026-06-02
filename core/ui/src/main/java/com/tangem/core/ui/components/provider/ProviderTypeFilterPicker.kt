package com.tangem.core.ui.components.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.core.ui.R
import com.tangem.domain.express.models.ProviderFilterType
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPicker
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemeRedesign
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun ProviderTypeFilterPicker(
    availableFilters: ImmutableList<ProviderFilterType>,
    selectedFilter: ProviderFilterType,
    onFilterSelect: (ProviderFilterType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val segments = remember(availableFilters) {
        availableFilters.map { filter ->
            TangemSegmentUM(
                id = filter.name,
                title = when (filter) {
                    ProviderFilterType.ALL -> resourceReference(R.string.common_all)
                    ProviderFilterType.CEX -> TextReference.Str("CEX")
                    ProviderFilterType.DEX -> TextReference.Str("DEX")
                },
            )
        }.toImmutableList()
    }
    val selectedSegment = remember(segments, selectedFilter) {
        segments.firstOrNull { it.id == selectedFilter.name }
    }
    TangemThemeRedesign {
        TangemSegmentedPicker(
            items = segments,
            initialSelectedItem = selectedSegment,
            isFixed = true,
            modifier = modifier,
            onClick = { segment ->
                val filterType = availableFilters.firstOrNull { it.name == segment.id }
                if (filterType != null) onFilterSelect(filterType)
            },
        )
    }
}