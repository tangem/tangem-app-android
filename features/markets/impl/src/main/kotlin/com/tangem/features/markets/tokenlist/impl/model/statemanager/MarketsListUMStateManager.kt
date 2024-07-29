package com.tangem.features.markets.tokenlist.impl.model.statemanager

import androidx.compose.runtime.Stable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.tokenlist.impl.ui.entity.SortByBottomSheetContentUM
import com.tangem.features.markets.tokenlist.impl.ui.entity.ListUM
import com.tangem.features.markets.tokenlist.impl.ui.entity.MarketsListItemUM
import com.tangem.features.markets.tokenlist.impl.ui.entity.MarketsListUM
import com.tangem.features.markets.tokenlist.impl.ui.entity.SortByTypeUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.*

@Stable
internal class MarketsListUMStateManager(
    private val onLoadMoreUiItems: () -> Unit,
    private val visibleItemsChanged: (itemsKeys: List<String>) -> Unit,
    private val onRetryButtonClicked: () -> Unit,
    private val onTokenClick: (MarketsListItemUM) -> Unit,
) {

    private var sortByBottomSheetIsShown
        get() = state.value.sortByBottomSheet.isShow
        set(value) = state.update { it.copy(sortByBottomSheet = it.sortByBottomSheet.copy(isShow = value)) }

    var searchQuery
        get() = state.value.searchBar.query
        private set(value) = state.update {
            it.copy(
                searchBar = it.searchBar.copy(
                    query = value,
                    isActive = value.isNotEmpty(),
                ),
            )
        }

    var isInSearchState
        get() = state.value.searchBar.isActive
        private set(value) = state.update { it.copy(searchBar = it.searchBar.copy(isActive = value)) }

    var selectedSortByType
        get() = state.value.selectedSortBy
        set(value) = state.update {
            it.copy(
                selectedSortBy = value,
                sortByBottomSheet = it.sortByBottomSheet.copy(
                    content = (it.sortByBottomSheet.content as SortByBottomSheetContentUM).copy(
                        selectedOption = value,
                    ),
                ),
                list = if (it.list is ListUM.Content && it.selectedSortBy != value) {
                    it.list.copy(triggerScrollReset = triggeredEvent(Unit) { consumeTriggerResetScrollEvent() })
                } else {
                    it.list
                },
            )
        }

    var selectedInterval
        get() = state.value.selectedInterval
        set(value) = state.update {
            it.copy(
                selectedInterval = value,
                list = if (it.list is ListUM.Content &&
                    it.selectedSortBy != SortByTypeUM.Rating &&
                    it.selectedInterval != value
                ) {
                    it.list.copy(triggerScrollReset = triggeredEvent(Unit) { consumeTriggerResetScrollEvent() })
                } else {
                    it.list
                },
            )
        }

    val state = MutableStateFlow(state())
    val isInSearchStateFlow = state.map { it.searchBar.isActive }.distinctUntilChanged()
    val searchQueryFlow = state.map { it.searchBar.query }.distinctUntilChanged()

    fun onUiItemsChanged(
        isInErrorState: Boolean,
        isSearchNotFound: Boolean,
        uiItems: ImmutableList<MarketsListItemUM>,
    ) {
        state.update {
            when {
                isInErrorState -> {
                    it.copy(
                        list = ListUM.LoadingError(onRetryClicked = onRetryButtonClicked),
                    )
                }
                isSearchNotFound -> {
                    it.copy(list = ListUM.SearchNothingFound)
                }
                uiItems.isEmpty() -> {
                    it.copy(list = ListUM.Loading)
                }
                else -> {
                    it.copy(
                        list = ListUM.Content(
                            items = uiItems,
                            loadMore = onLoadMoreUiItems,
                            visibleIdsChanged = visibleItemsChanged,
                            showUnder100kTokens = true,
                            onShowTokensUnder100kClicked = { },
                            triggerScrollReset = consumedEvent(),
                            onItemClick = onTokenClick,
                        ),
                    )
                }
            }
        }
    }

    private fun state(): MarketsListUM = MarketsListUM(
        list = ListUM.Loading,
        searchBar = SearchBarUM(
            placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
            query = "",
            onQueryChange = { searchQuery = it },
            isActive = false,
            onActiveChange = { },
        ),
        selectedSortBy = SortByTypeUM.Rating,
        selectedInterval = MarketsListUM.TrendInterval.H24,
        onIntervalClick = { selectedInterval = it },
        onSortByButtonClick = { sortByBottomSheetIsShown = true },
        sortByBottomSheet = TangemBottomSheetConfig(
            isShow = false,
            onDismissRequest = { sortByBottomSheetIsShown = false },
            content = SortByBottomSheetContentUM(
                selectedOption = SortByTypeUM.Rating,
                onOptionClicked = ::onBottomSheetOptionClicked,
            ),
        ),
    )

    private fun onBottomSheetOptionClicked(sortByTypeUM: SortByTypeUM) {
        state.update {
            it.copy(
                selectedSortBy = sortByTypeUM,
                sortByBottomSheet = it.sortByBottomSheet.copy(
                    isShow = false,
                    content = (it.sortByBottomSheet.content as SortByBottomSheetContentUM).copy(
                        selectedOption = sortByTypeUM,
                    ),
                ),
            )
        }
    }

    private fun consumeTriggerResetScrollEvent() {
        state.update {
            it.copy(
                list = if (it.list is ListUM.Content) {
                    it.list.copy(
                        triggerScrollReset = consumedEvent(),
                    )
                } else {
                    it.list
                },
            )
        }
    }
}
