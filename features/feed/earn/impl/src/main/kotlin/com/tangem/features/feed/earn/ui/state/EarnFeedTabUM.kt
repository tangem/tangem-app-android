package com.tangem.features.feed.earn.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class EarnFeedTabUM(
    val mostlyUsed: EarnListUM,
    val bestOpportunities: EarnBestOpportunitiesUM,
    val filters: ImmutableList<TangemFilterItemUM>,
    val onSliderScroll: () -> Unit,
)