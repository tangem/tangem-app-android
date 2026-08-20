package com.tangem.features.feed.earn.model.statemanager

import com.tangem.pagination.PaginationStatus
import com.tangem.features.feed.earn.ui.state.EarnBestOpportunitiesUM
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongParameterList")
internal object EarnListStateManager {

    fun calculateState(
        items: ImmutableList<TangemTokenRow.State.Content>,
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