package com.tangem.features.feed.ui.earn.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class EarnUM(
    val mostlyUsed: EarnListUM,
    val bestOpportunities: EarnBestOpportunitiesUM,
    val selectedTypeFilter: EarnFilterTypeUM,
    val selectedNetworkFilter: EarnFilterNetworkUM,
    val onBackClick: () -> Unit,
    val onNetworkFilterClick: () -> Unit,
    val onTypeFilterClick: () -> Unit,
    val onSliderScroll: () -> Unit,
) {

    val selectedTypeFilterText: TextReference
        get() = selectedTypeFilter.text
}