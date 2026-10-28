package com.tangem.core.ui.components.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.express.models.ProviderFilterType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun ProviderTypeFilterPicker(
    availableFilters: ImmutableList<ProviderFilterType>,
    selectedFilter: ProviderFilterType,
    onFilterSelect: (ProviderFilterType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember(availableFilters, selectedFilter, onFilterSelect) {
        availableFilters.map { filter ->
            TangemSegmentedControl.Item(
                id = filter.name,
                label = when (filter) {
                    ProviderFilterType.ALL -> resourceReference(R.string.common_all)
                    ProviderFilterType.CEX -> TextReference.Str("CEX")
                    ProviderFilterType.DEX -> TextReference.Str("DEX")
                },
                isSelected = filter == selectedFilter,
                onClick = { onFilterSelect(filter) },
            )
        }.toImmutableList()
    }
    TangemSegmentedControl(items = items, modifier = modifier)
}