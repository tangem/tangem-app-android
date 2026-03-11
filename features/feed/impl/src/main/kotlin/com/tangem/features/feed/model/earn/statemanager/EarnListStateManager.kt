package com.tangem.features.feed.model.earn.statemanager

import com.tangem.pagination.PaginationStatus
import com.tangem.features.feed.ui.earn.state.EarnBestOpportunitiesUM
import com.tangem.features.feed.ui.earn.state.EarnListItemUM
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongParameterList")
internal object EarnListStateManager {

    fun calculateState(
        items: ImmutableList<EarnListItemUM>,
        error: Throwable?,
        paginationStatus: PaginationStatus<*>,
        hasActiveFilters: Boolean,
        onRetryClick: () -> Unit,
        onLoadMore: () -> Unit,
        onClearFiltersClick: () -> Unit,
    ): EarnBestOpportunitiesUM = when {
        error != null -> EarnBestOpportunitiesUM.Error(onRetryClicked = onRetryClick)
        paginationStatus is PaginationStatus.InitialLoading && items.isEmpty() ->
            EarnBestOpportunitiesUM.Loading
        items.isEmpty() && hasActiveFilters ->
            EarnBestOpportunitiesUM.EmptyFiltered(onClearFilterClick = onClearFiltersClick)
        items.isEmpty() -> EarnBestOpportunitiesUM.Empty
        else -> EarnBestOpportunitiesUM.Content(items = items, onLoadMore = onLoadMore)
    }
}