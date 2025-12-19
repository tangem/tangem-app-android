package com.tangem.features.feed.model.market.list.statemanager

import androidx.compose.runtime.Stable
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.list.state.*
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Stable
@Suppress("LongParameterList", "LargeClass")
internal class MarketsListUMStateManager(
    private val shouldAlwaysShowSearchBar: Provider<Boolean>,
    private val currentVisibleIds: Provider<List<CryptoCurrency.RawID>>,
    private val preselectedSortType: Provider<SortByTypeUM>,
    private val onLoadMoreUiItems: () -> Unit,
    private val visibleItemsChanged: (itemsKeys: List<CryptoCurrency.RawID>) -> Unit,
    private val onRetryButtonClicked: () -> Unit,
    private val onTokenClick: (MarketsListItemUM) -> Unit,
    private val onShowTokensUnder100kClicked: () -> Unit,
    private val onBackClick: () -> Unit,
) {

    val state = MutableStateFlow(state())

    private var isSortByBottomSheetShown
        get() = state.value.sortByBottomSheet.isShown
        set(value) = state.update { it.copy(sortByBottomSheet = it.sortByBottomSheet.copy(isShown = value)) }

    var searchQuery
        get() = state.value.marketsSearchBar.searchBarUM.query
        private set(value) = state.update { marketsListUM ->
            marketsListUM.copy(
                marketsSearchBar = marketsListUM.marketsSearchBar.copy(
                    searchBarUM = marketsListUM.marketsSearchBar.searchBarUM.copy(query = value),
                ),
            )
        }

    val isInSearchState
        get() = state.value.marketsSearchBar.searchBarUM.isActive

    var selectedSortByType
        get() = state.value.selectedSortBy
        set(value) = state.update { marketsListUM ->
            marketsListUM.copy(
                selectedSortBy = value,
                sortByBottomSheet = marketsListUM.sortByBottomSheet.copy(
                    content = (marketsListUM.sortByBottomSheet.content as SortByBottomSheetContentUM).copy(
                        selectedOption = value,
                    ),
                ),
                list = if (marketsListUM.list is ListUM.Content && marketsListUM.selectedSortBy != value) {
                    marketsListUM.list.copy(
                        triggerScrollReset = triggeredEvent(Unit) { consumeTriggerResetScrollEvent() },
                    )
                } else {
                    marketsListUM.list
                },
            )
        }

    var selectedInterval
        get() = state.value.selectedInterval
        set(value) = state.update { marketsListUM ->
            marketsListUM.copy(
                selectedInterval = value,
                list = if (marketsListUM.list is ListUM.Content &&
                    marketsListUM.selectedSortBy != SortByTypeUM.Rating &&
                    marketsListUM.selectedInterval != value
                ) {
                    marketsListUM.list.copy(
                        triggerScrollReset = triggeredEvent(Unit) { consumeTriggerResetScrollEvent() },
                    )
                } else {
                    marketsListUM.list
                },
            )
        }

    val isInSearchStateFlow = state.map { it.isInSearchMode }.distinctUntilChanged()
    val searchQueryFlow = state.map { it.marketsSearchBar.searchBarUM.query }.distinctUntilChanged()

    fun onUiItemsChanged(
        isInErrorState: Boolean,
        isSearchNotFound: Boolean,
        uiItems: ImmutableList<MarketsListItemUM>,
        marketsNotificationUM: MarketsNotificationUM?,
    ) {
        state.update { marketsListUM ->
            when {
                isInErrorState -> {
                    marketsListUM.copy(
                        list = ListUM.LoadingError(onRetryClicked = onRetryButtonClicked),
                    )
                }
                isSearchNotFound -> {
                    marketsListUM.copy(list = ListUM.SearchNothingFound)
                }
                uiItems.isEmpty() -> {
                    marketsListUM.copy(list = ListUM.Loading)
                }
                else -> {
                    marketsListUM.updateItems(
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
                        shouldShowUnder100kTokensNotificationWasHidden = currentState
                            .showUnder100kButtonAlreadyPressed(),
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
                    shouldShowUnder100kTokensNotificationWasHidden = false,
                    shouldShowUnder100kTokensNotification = true,
                    onShowTokensUnder100kClicked = {
                        onShowTokensUnder100kClicked()
                        state.update { s ->
                            (s.list as? ListUM.Content)?.let {
                                s.copy(
                                    list = s.list.copy(
                                        items = searchUiItemsCached,
                                        shouldShowUnder100kTokensNotification = false,
                                        shouldShowUnder100kTokensNotificationWasHidden = true,
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
        return this.list is ListUM.Content &&
            this.isInSearchMode &&
            this.list.shouldShowUnder100kTokensNotificationWasHidden
    }

    // Show price change animation for visible items only
    private fun ImmutableList<MarketsListItemUM>.filterPriceChangeByVisibility(): ImmutableList<MarketsListItemUM> {
        val visibleItemIds = currentVisibleIds()
        return map { marketsListItemUM ->
            marketsListItemUM.copy(
                price = marketsListItemUM.price.copy(
                    changeType = if (visibleItemIds.contains(marketsListItemUM.id)) {
                        marketsListItemUM.price.changeType
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
            shouldShowUnder100kTokensNotificationWasHidden = false,
        )
    }

    private fun state(): MarketsListUM = MarketsListUM(
        list = ListUM.Loading,
        marketsSearchBar = MarketsSearchBar(
            searchBarUM = SearchBarUM(
                placeholderText = resourceReference(R.string.markets_search_header_title),
                query = "",
                onQueryChange = { searchQuery = it },
                isActive = false,
                onActiveChange = ::changeSearchBarIsActive,
                onClearClick = {
                    if (shouldAlwaysShowSearchBar()) {
                        onBackClick()
                    }
                },
            ),
            shouldAlwaysShowSearchBar = shouldAlwaysShowSearchBar(),
        ),
        selectedSortBy = preselectedSortType(),
        selectedInterval = MarketsListUM.TrendInterval.H24,
        onIntervalClick = { selectedInterval = it },
        onSortByButtonClick = { isSortByBottomSheetShown = true },
        sortByBottomSheet = TangemBottomSheetConfig(
            isShown = false,
            onDismissRequest = { isSortByBottomSheetShown = false },
            content = SortByBottomSheetContentUM(
                selectedOption = preselectedSortType(),
                onOptionClicked = ::onBottomSheetOptionClicked,
            ),
        ),
        marketsNotificationUM = null,
        onSearchClicked = { changeSearchBarIsActive(true) },
    )

    private fun onBottomSheetOptionClicked(sortByTypeUM: SortByTypeUM) {
        state.update { marketsListUM ->
            marketsListUM.copy(
                selectedSortBy = sortByTypeUM,
                sortByBottomSheet = marketsListUM.sortByBottomSheet.copy(
                    isShown = false,
                    content = (marketsListUM.sortByBottomSheet.content as SortByBottomSheetContentUM).copy(
                        selectedOption = sortByTypeUM,
                    ),
                ),
            )
        }
    }

    private fun consumeTriggerResetScrollEvent() {
        state.update { marketsListUM ->
            marketsListUM.copy(
                list = if (marketsListUM.list is ListUM.Content) {
                    marketsListUM.list.copy(
                        triggerScrollReset = consumedEvent(),
                    )
                } else {
                    marketsListUM.list
                },
            )
        }
    }

    private fun changeSearchBarIsActive(isActive: Boolean) {
        state.update { marketsListUM ->
            marketsListUM.copy(
                marketsSearchBar = marketsListUM.marketsSearchBar.copy(
                    searchBarUM = marketsListUM.marketsSearchBar.searchBarUM.copy(isActive = isActive),
                ),
            )
        }
    }
}