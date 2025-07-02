package com.tangem.features.markets.tokenlist.impl.model.statemanager

import androidx.compose.runtime.Stable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.tokenlist.impl.ui.state.*
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

@Stable
@Suppress("LongParameterList")
internal class MarketsListUMStateManager(
    private val currentVisibleIds: Provider<List<CryptoCurrency.RawID>>,
    private val onLoadMoreUiItems: () -> Unit,
    private val visibleItemsChanged: (itemsKeys: List<CryptoCurrency.RawID>) -> Unit,
    private val onRetryButtonClicked: () -> Unit,
    private val onTokenClick: (MarketsListItemUM) -> Unit,
    private val onStakingNotificationClick: () -> Unit,
    private val onStakingNotificationCloseClick: () -> Unit,
    private val onShowTokensUnder100kClicked: () -> Unit,
) {

    private var sortByBottomSheetIsShown
        get() = state.value.sortByBottomSheet.isShown
        set(value) = state.update { it.copy(sortByBottomSheet = it.sortByBottomSheet.copy(isShown = value)) }

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
        stakingNotificationMaxApy: BigDecimal?,
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
                    it.updateItems(
                        newItems = uiItems,
                        stakingNotificationMaxApy = stakingNotificationMaxApy,
                    )
                }
            }
        }
    }

    private fun MarketsListUM.updateItems(
        newItems: ImmutableList<MarketsListItemUM>,
        stakingNotificationMaxApy: BigDecimal?,
    ): MarketsListUM {
        val currentState = this

        if (isInSearchMode.not() || currentState.showUnder100kButtonAlreadyPressed()) {
            val itemsWithFilteredPriceChange = newItems.filterPriceChangeByVisibility()

            return currentState.copy(
                list = generalContentState(itemsWithFilteredPriceChange)
                    .copy(
                        showUnder100kTokensNotificationWasHidden = currentState.showUnder100kButtonAlreadyPressed(),
                    ),
                stakingNotificationMaxApy = stakingNotificationMaxApy,
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
                    showUnder100kTokensNotificationWasHidden = false,
                    showUnder100kTokensNotification = true,
                    onShowTokensUnder100kClicked = {
                        onShowTokensUnder100kClicked()
                        state.update { s ->
                            (s.list as? ListUM.Content)?.let {
                                s.copy(
                                    list = s.list.copy(
                                        items = searchUiItemsCached,
                                        showUnder100kTokensNotification = false,
                                        showUnder100kTokensNotificationWasHidden = true,
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
        return this.list is ListUM.Content && this.isInSearchMode && this.list.showUnder100kTokensNotificationWasHidden
    }

    // Show price change animation for visible items only
    private fun ImmutableList<MarketsListItemUM>.filterPriceChangeByVisibility(): ImmutableList<MarketsListItemUM> {
        val visibleItemIds = currentVisibleIds()
        return map {
            it.copy(
                price = it.price.copy(
                    changeType = if (visibleItemIds.contains(it.id)) {
                        it.price.changeType
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
            showUnder100kTokensNotification = false,
            onShowTokensUnder100kClicked = {},
            triggerScrollReset = consumedEvent(),
            onItemClick = onTokenClick,
            showUnder100kTokensNotificationWasHidden = false,
        )
    }

    private fun state(): MarketsListUM = MarketsListUM(
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
        onSortByButtonClick = { sortByBottomSheetIsShown = true },
        sortByBottomSheet = TangemBottomSheetConfig(
            isShown = false,
            onDismissRequest = { sortByBottomSheetIsShown = false },
            content = SortByBottomSheetContentUM(
                selectedOption = SortByTypeUM.Rating,
                onOptionClicked = ::onBottomSheetOptionClicked,
            ),
        ),
        stakingNotificationMaxApy = null,
        onStakingNotificationClick = {
            onStakingNotificationClick()
            selectedSortByType = SortByTypeUM.Staking
        },
        onStakingNotificationCloseClick = onStakingNotificationCloseClick,
    )

    private fun onBottomSheetOptionClicked(sortByTypeUM: SortByTypeUM) {
        state.update {
            it.copy(
                selectedSortBy = sortByTypeUM,
                sortByBottomSheet = it.sortByBottomSheet.copy(
                    isShown = false,
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