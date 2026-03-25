package com.tangem.features.feed.ui.earn.state

import androidx.compose.runtime.Immutable

@Immutable
internal data class EarnFilterUM(
    val selectedTypeFilter: EarnFilterTypeUM,
    val selectedNetworkFilter: EarnFilterNetworkUM,
    val isTypeFilterEnabled: Boolean = true,
    val isNetworkFilterEnabled: Boolean = true,
)