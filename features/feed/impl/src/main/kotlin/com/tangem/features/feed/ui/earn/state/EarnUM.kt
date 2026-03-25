package com.tangem.features.feed.ui.earn.state

import androidx.compose.runtime.Immutable

@Immutable
internal data class EarnUM(
    val mostlyUsed: EarnListUM,
    val bestOpportunities: EarnBestOpportunitiesUM,
    val earnFilterUM: EarnFilterUM,
    val onBackClick: () -> Unit,
    val onNetworkFilterClick: () -> Unit,
    val onTypeFilterClick: () -> Unit,
    val onSliderScroll: () -> Unit,
)