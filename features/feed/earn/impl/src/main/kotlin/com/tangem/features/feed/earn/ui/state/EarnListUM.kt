package com.tangem.features.feed.earn.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface EarnListUM {
    data object Loading : EarnListUM
    data class Content(val items: ImmutableList<EarnOpportunitiesItemUM>) : EarnListUM
    data class Error(val onRetryClicked: () -> Unit) : EarnListUM
    data object Empty : EarnListUM
}

@Immutable
internal sealed interface EarnBestOpportunitiesUM {
    data object Loading : EarnBestOpportunitiesUM
    data object Empty : EarnBestOpportunitiesUM
    data class EmptyFiltered(val onClearFilterClick: () -> Unit) : EarnBestOpportunitiesUM
    data class Content(
        val items: ImmutableList<TangemTokenRow.State.Content>,
        val onLoadMore: () -> Unit,
    ) : EarnBestOpportunitiesUM

    data class Error(val onRetryClicked: () -> Unit) : EarnBestOpportunitiesUM
}

@Immutable
internal data class EarnOpportunitiesItemUM(
    val id: String,
    val currencyIconState: CurrencyIconState,
    val tokenName: TextReference,
    val symbol: TextReference,
    val earnValue: TextReference,
    val earnType: TextReference,
    val onItemClick: () -> Unit,
)