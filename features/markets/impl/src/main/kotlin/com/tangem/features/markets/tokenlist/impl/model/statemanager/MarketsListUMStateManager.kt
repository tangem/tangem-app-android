package com.tangem.features.markets.tokenlist.impl.model.statemanager

import androidx.compose.runtime.Stable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.tokenlist.impl.model.MarketsNotificationUM
import com.tangem.features.markets.tokenlist.impl.ui.state.*
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Stable
@Suppress("LongParameterList")
internal class MarketsListUMStateManager(
    private val currentVisibleIds: Provider<List<CryptoCurrency.RawID>>,
    private val onLoadMoreUiItems: () -> Unit,
    private val visibleItemsChanged: (itemsKeys: List<CryptoCurrency.RawID>) -> Unit,
    private val onRetryButtonClicked: () -> Unit,
    private val onTokenClick: (MarketsListItemUM) -> Unit,
    private val onShowTokensUnder100kClicked: () -> Unit,
) {

    val state = MutableStateFlow(createInitialState())
    val isInSearchStateFlow = state.map { it.searchBar.isActive }.distinctUntilChanged()
    val searchQueryFlow = state.map { it.searchBar.query }.distinctUntilChanged()

    private var isSortByBottomSheetShown
        get() = state.value.sortByBottomSheet.isShown
        set(value) = state.update { it.copy(sortByBottomSheet = it.sortByBottomSheet.copy(isShown = value)) }

    var searchQuery
        get() = state.value.searchBar.query
        private set(value) = state.update { currentState ->
            currentState.copy(
                searchBar = currentState.searchBar.copy(
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
        set(value) = state.update { currentState ->
            currentState.copy(
                selectedSortBy = value,
                sortByBottomSheet = currentState.sortByBottomSheet.copy(
                    content = (currentState.sortByBottomSheet.content as SortByBottomSheetContentUM).copy(
                        selectedOption = value,
                    ),
                ),
                list = if (currentState.list is ListUM.Content && currentState.selectedSortBy != value) {
                    currentState.list.copy(
                        triggerScrollReset = triggeredEvent(Unit) { consumeTriggerResetScrollEvent() },
                    )
                } else {
                    currentState.list
                },
            )
        }

    var selectedInterval
        get() = state.value.selectedInterval
        set(value) = state.update { currentState ->
            currentState.copy(
                selectedInterval = value,
                list = if (currentState.list is ListUM.Content &&
                    currentState.selectedSortBy != SortByTypeUM.Rating &&
                    currentState.selectedInterval != value
                ) {
                    currentState.list.copy(
                        triggerScrollReset = triggeredEvent(Unit) { consumeTriggerResetScrollEvent() },
                    )
                } else {
                    currentState.list
                },
            )
        }

    fun onUiItemsChanged(
        isInErrorState: Boolean,
        isSearchNotFound: Boolean,
        uiItems: ImmutableList<MarketsListItemUM>,
        marketsNotificationUM: MarketsNotificationUM?,
    ) {
        state.update { currentState ->
            when {
                isInErrorState -> {
                    currentState.copy(
                        list = ListUM.LoadingError(onRetryClicked = onRetryButtonClicked),
                    )
                }
                isSearchNotFound -> {
                    currentState.copy(list = ListUM.SearchNothingFound)
                }
                uiItems.isEmpty() -> {
                    currentState.copy(list = ListUM.Loading)
                }
                else -> {
                    currentState.updateItems(
                        newItems = uiItems,
                        marketsNotificationUM = marketsNotificationUM,
                    )
                }
            }
        }
    }

    private fun MarketsListUM.updateItems(
        newItems: ImmutableList<MarketsListItemUM>,
        marketsNotificationUM: MarketsNotificationUM?,
    ): MarketsListUM {
        val currentState = this

        if (isInSearchMode.not() || currentState.showUnder100kButtonAlreadyPressed()) {
            val itemsWithFilteredPriceChange = newItems.filterPriceChangeByVisibility()

            return currentState.copy(
                list = generalContentState(itemsWithFilteredPriceChange)
                    .copy(
                        wasUnder100kTokensNotificationHidden = currentState.showUnder100kButtonAlreadyPressed(),
                    ),
                marketsNotificationUM = marketsNotificationUM,
            )
        }

        // Search state cases

        val filtered = newItems.filter { item -> item.isUnder100kMarketCap.not() }
            .toImmutableList()
            .filterPriceChangeByVisibility()

        if (filtered.size != newItems.size) {
            val searchUiItemsCached = newItems.filterPriceChangeByVisibility()

            return currentState.copy(
                list = generalContentState(filtered).copy(
                    wasUnder100kTokensNotificationHidden = false,
                    shouldShowUnder100kTokensNotification = true,
                    onShowTokensUnder100kClicked = {
                        onShowTokensUnder100kClicked()
                        state.update { s ->
                            (s.list as? ListUM.Content)?.let {
                                s.copy(
                                    list = s.list.copy(
                                        items = searchUiItemsCached,
                                        shouldShowUnder100kTokensNotification = false,
                                        wasUnder100kTokensNotificationHidden = true,
                                    ),
                                )
                            } ?: s
                        }
                    },
                ),
            )
        } else {
            return currentState.copy(
                list = generalContentState(newItems.filterPriceChangeByVisibility()),
            )
        }
    }

    private fun MarketsListUM.showUnder100kButtonAlreadyPressed(): Boolean {
        return this.list is ListUM.Content && this.isInSearchMode && this.list.wasUnder100kTokensNotificationHidden
    }

    // Show price change animation for visible items only
    private fun ImmutableList<MarketsListItemUM>.filterPriceChangeByVisibility(): ImmutableList<MarketsListItemUM> {
        val visibleItemIds = currentVisibleIds()
        return map { item ->
            item.copy(
                price = item.price.copy(
                    changeType = if (visibleItemIds.contains(item.id)) {
                        item.price.changeType
                    } else {
                        null
                    },
                ),
            )
        }.toImmutableList()
    }

    private fun generalContentState(newItems: ImmutableList<MarketsListItemUM>): ListUM.Content {
        return ListUM.Content(
            items = newItems,
            loadMore = onLoadMoreUiItems,
            visibleIdsChanged = visibleItemsChanged,
            shouldShowUnder100kTokensNotification = false,
            onShowTokensUnder100kClicked = {},
            triggerScrollReset = consumedEvent(),
            onItemClick = onTokenClick,
            wasUnder100kTokensNotificationHidden = false,
        )
    }

    private fun onBottomSheetOptionClicked(sortByTypeUM: SortByTypeUM) {
        state.update { currentState ->
            currentState.copy(
                selectedSortBy = sortByTypeUM,
                sortByBottomSheet = currentState.sortByBottomSheet.copy(
                    isShown = false,
                    content = (currentState.sortByBottomSheet.content as SortByBottomSheetContentUM).copy(
                        selectedOption = sortByTypeUM,
                    ),
                ),
            )
        }
    }

    private fun consumeTriggerResetScrollEvent() {
        state.update { currentState ->
            currentState.copy(
                list = if (currentState.list is ListUM.Content) {
                    currentState.list.copy(
                        triggerScrollReset = consumedEvent(),
                    )
                } else {
                    currentState.list
                },
            )
        }
    }

    private fun createInitialState(): MarketsListUM = MarketsListUM(
        list = ListUM.Loading,
        searchBar = SearchBarUM(
            placeholderText = resourceReference(R.string.markets_search_header_title),
            query = "",
            onQueryChange = { searchQuery = it },
            isActive = false,
            onActiveChange = { },
        ),
        selectedSortBy = SortByTypeUM.Rating,
        selectedInterval = MarketsListUM.TrendInterval.H24,
        onIntervalClick = { selectedInterval = it },
        onSortByButtonClick = { isSortByBottomSheetShown = true },
        sortByBottomSheet = TangemBottomSheetConfig(
            isShown = false,
            onDismissRequest = { isSortByBottomSheetShown = false },
            content = SortByBottomSheetContentUM(
                selectedOption = SortByTypeUM.Rating,
                onOptionClicked = ::onBottomSheetOptionClicked,
            ),
        ),
        marketsNotificationUM = null,
    )
}