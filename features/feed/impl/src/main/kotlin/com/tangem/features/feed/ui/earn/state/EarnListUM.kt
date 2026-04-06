package com.tangem.features.feed.ui.earn.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.earn.EarnType
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface EarnListUM {
    data object Loading : EarnListUM
    data class Content(val items: ImmutableList<EarnListItemUM>) : EarnListUM
    data class Error(val onRetryClicked: () -> Unit) : EarnListUM
    data object Empty : EarnListUM
}

@Immutable
internal sealed interface EarnBestOpportunitiesUM {
    data object Loading : EarnBestOpportunitiesUM
    data object Empty : EarnBestOpportunitiesUM
    data class EmptyFiltered(val onClearFilterClick: () -> Unit) : EarnBestOpportunitiesUM
    data class Content(
        val items: ImmutableList<EarnListItemUM>,
        val onLoadMore: () -> Unit,
    ) : EarnBestOpportunitiesUM
    data class Error(val onRetryClicked: () -> Unit) : EarnBestOpportunitiesUM
}

@Immutable
internal data class EarnListItemUM(
    val network: TextReference,
    val symbol: TextReference,
    val tokenName: TextReference,
    val currencyIconState: CurrencyIconState,
    val earnValue: TextReference,
    val earnType: EarnType,
    val earnTypeTitle: TextReference,
    val onItemClick: () -> Unit,
)