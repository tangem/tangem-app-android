package com.tangem.features.feed.earn.model.statemanager

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.feed.earn.ui.state.EarnBestOpportunitiesUM
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * The expected states are built with the very same lambda instances that are handed to
 * [EarnListStateManager.calculateState], so a whole-object comparison also proves each callback landed
 * in its own field rather than merely that some lambda did.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnListStateManagerTest {

    private val onRetryClick: () -> Unit = {}
    private val onLoadMore: () -> Unit = {}
    private val onClearFiltersClick: () -> Unit = {}

    @ParameterizedTest
    @ProvideTestModels
    fun calculateState(model: StateModel) {
        // Act
        val actual = EarnListStateManager.calculateState(
            items = model.items,
            error = model.error,
            paginationStatus = model.paginationStatus,
            hasActiveFilters = model.hasActiveFilters,
            onRetryClick = onRetryClick,
            onLoadMore = onLoadMore,
            onClearFiltersClick = onClearFiltersClick,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        StateModel(
            name = "GIVEN an error WHEN state calculated THEN the error wins over the loaded items",
            items = items,
            error = error,
            paginationStatus = PaginationStatus.Paginating(lastResult = successResult),
            expected = EarnBestOpportunitiesUM.Error(onRetryClicked = onRetryClick),
        ),
        StateModel(
            name = "GIVEN an error WHEN the first page is still loading THEN the error wins over loading",
            items = persistentListOf(),
            error = error,
            paginationStatus = PaginationStatus.InitialLoading,
            hasActiveFilters = true,
            expected = EarnBestOpportunitiesUM.Error(onRetryClicked = onRetryClick),
        ),
        StateModel(
            name = "GIVEN no items WHEN the first page is loading THEN the list shimmers",
            items = persistentListOf(),
            error = null,
            paginationStatus = PaginationStatus.InitialLoading,
            expected = EarnBestOpportunitiesUM.Loading,
        ),
        StateModel(
            name = "GIVEN no items and active filters WHEN the first page is loading THEN loading wins",
            items = persistentListOf(),
            error = null,
            paginationStatus = PaginationStatus.InitialLoading,
            hasActiveFilters = true,
            expected = EarnBestOpportunitiesUM.Loading,
        ),
        StateModel(
            name = "GIVEN items already shown WHEN the first page is loading THEN the items win over loading",
            items = items,
            error = null,
            paginationStatus = PaginationStatus.InitialLoading,
            expected = EarnBestOpportunitiesUM.Content(items = items, onLoadMore = onLoadMore),
        ),
        StateModel(
            name = "GIVEN an empty result and active filters WHEN state calculated THEN the filters can be cleared",
            items = persistentListOf(),
            error = null,
            paginationStatus = PaginationStatus.EndOfPagination,
            hasActiveFilters = true,
            expected = EarnBestOpportunitiesUM.EmptyFiltered(onClearFilterClick = onClearFiltersClick),
        ),
        StateModel(
            name = "GIVEN an empty result and no filters WHEN state calculated THEN the plain empty state is shown",
            items = persistentListOf(),
            error = null,
            paginationStatus = PaginationStatus.EndOfPagination,
            expected = EarnBestOpportunitiesUM.Empty,
        ),
        StateModel(
            name = "GIVEN items and active filters WHEN state calculated THEN the items are shown",
            items = items,
            error = null,
            paginationStatus = PaginationStatus.EndOfPagination,
            hasActiveFilters = true,
            expected = EarnBestOpportunitiesUM.Content(items = items, onLoadMore = onLoadMore),
        ),
    )

    internal data class StateModel(
        val name: String,
        val items: ImmutableList<TangemTokenRow.State.Content>,
        val error: Throwable?,
        val paginationStatus: PaginationStatus<*>,
        val hasActiveFilters: Boolean = false,
        val expected: EarnBestOpportunitiesUM,
    ) {
        override fun toString(): String = name
    }

    private companion object {

        val error = IllegalStateException("boom")

        val items: ImmutableList<TangemTokenRow.State.Content> = persistentListOf(
            TangemTokenRow.State.Content(
                id = "coin-ethereum_STAKING",
                icon = TangemTokenIcon.UiState.Token(tokenState = TangemTokenIcon.State(url = null)),
                title = stringReference("Ethereum"),
            ),
        )

        val successResult = BatchFetchResult.Success(data = emptyList<Nothing>(), empty = true, last = false)
    }
}