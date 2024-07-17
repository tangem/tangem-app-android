package com.tangem.features.markets.model.statemanager

import androidx.compose.runtime.Stable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.model.SortByBottomSheetContentUM
import com.tangem.features.markets.ui.entity.ListUM
import com.tangem.features.markets.ui.entity.MarketsListItemUM
import com.tangem.features.markets.ui.entity.MarketsListUM
import com.tangem.features.markets.ui.entity.SortByTypeUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Stable
internal class MarketsListUMStateManager(
    private val onLoadMoreUiItems: () -> Unit,
    private val visibleItemsChanged: (itemsKeys: List<String>) -> Unit,
) {

    private var sortByBottomSheetIsShown
        get() = state.value.sortByBottomSheet.isShow
        set(value) = state.update { it.copy(sortByBottomSheet = it.sortByBottomSheet.copy(isShow = value)) }

    private val isInSearchStateFlow = MutableStateFlow(false)

    var isInSearchState
        get() = isInSearchStateFlow.value
        set(value) { isInSearchStateFlow.value = value }

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
            )
        }

    var selectedInterval
        get() = state.value.selectedInterval
        set(value) = state.update { it.copy(selectedInterval = value) }

    val state = MutableStateFlow(state())

    fun onUiItemsChanged(uiItems: ImmutableList<MarketsListItemUM>) {
        state.update {
            if (uiItems.isEmpty()) {
                it.copy(
                    list = ListUM.Loading,
                )
            } else {
                it.copy(
                    list = ListUM.Content(
                        items = uiItems,
                        loadMore = onLoadMoreUiItems,
                        visibleIdsChanged = visibleItemsChanged,
                    ),
                )
            }
        }
    }

    private fun state(): MarketsListUM = MarketsListUM(
        list = ListUM.Loading,
        searchBar = SearchBarUM(
            placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
            query = "", // TODO
            onQueryChange = {}, // TODO
            isActive = false, // TODO
            onActiveChange = { }, // TODO
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
}