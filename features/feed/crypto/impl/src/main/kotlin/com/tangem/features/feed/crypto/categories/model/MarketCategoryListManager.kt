package com.tangem.features.feed.crypto.categories.model

import com.tangem.domain.markets.GetMarketCategoryListFlowUseCase
import com.tangem.domain.markets.MarketCategory
import com.tangem.domain.markets.MarketCategoryListBatchingContext
import com.tangem.domain.markets.MarketCategoryListConfig
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.PaginationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Paginated market-category list of the selector screen.
 *
 * Categories carry no live-updating data (`TUpdate` is [Nothing]), so batches are flattened straight
 * to domain models. The diff-updating `BatchListStateManager` the markets lists use exists to
 * reconcile changing quotes on already-rendered rows and would buy nothing here.
 */
internal class MarketCategoryListManager(
    getMarketCategoryListFlowUseCase: GetMarketCategoryListFlowUseCase,
    private val currentAppCurrencyCode: () -> String,
    private val modelScope: CoroutineScope,
) {

    private val actionsFlow = MutableSharedFlow<BatchAction<Int, MarketCategoryListConfig, Nothing>>()

    private val batchFlow = getMarketCategoryListFlowUseCase(
        batchingContext = MarketCategoryListBatchingContext(
            actionsFlow = actionsFlow,
            coroutineScope = modelScope,
        ),
    )

    val categories: StateFlow<List<MarketCategory>> = batchFlow.state
        .map { state -> state.data.flatMap(Batch<Int, List<MarketCategory>>::data) }
        .distinctUntilChanged()
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = emptyList())

    val status: StateFlow<PaginationStatus<List<MarketCategory>>> = batchFlow.state
        .map { it.status }
        .distinctUntilChanged()
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = PaginationStatus.None)

    /**
     * The selector lists every category, so the request stays at [MarketCategoryListConfig.FeaturedFilter.All];
     * the featured rail is a separate `featured = true` request, not a slice of this one.
     *
     * `timestamp` is left unset: the categories response echoes no snapshot token, so there is no
     * value to hold stable across pages.
     *
     * @param searchText narrows the listing; `null` lists every category
     */
    fun reload(searchText: String? = null) {
        modelScope.launch {
            actionsFlow.emit(
                BatchAction.Reload(
                    requestParams = MarketCategoryListConfig(
                        fiatCurrencySymbol = currentAppCurrencyCode(),
                        searchText = searchText,
                    ),
                ),
            )
        }
    }

    fun loadMore() {
        modelScope.launch { actionsFlow.emit(BatchAction.LoadMore()) }
    }
}