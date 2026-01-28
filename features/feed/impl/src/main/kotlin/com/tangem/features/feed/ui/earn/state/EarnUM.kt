package com.tangem.features.feed.ui.earn.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class EarnUM(
    val mostlyUsed: EarnListUM,
    val bestOpportunities: EarnListUM,
    val selectedNetworkFilter: EarnFilterUM?,
    val selectedTypeFilter: EarnFilterUM?,
    val networkFilters: ImmutableList<EarnFilterUM>,
    val typeFilters: ImmutableList<EarnFilterUM>,
    val onBackClick: () -> Unit,
    val onNetworkFilterClick: () -> Unit,
    val onTypeFilterClick: () -> Unit,
)

@Immutable
internal data class EarnFilterUM(
    val id: String,
    val name: TextReference,
    val isSelected: Boolean,
    val onClick: () -> Unit,
)