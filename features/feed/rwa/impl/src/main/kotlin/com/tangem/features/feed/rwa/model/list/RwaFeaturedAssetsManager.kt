package com.tangem.features.feed.rwa.model.list

import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.rwa.ui.state.RwaFeaturedAssetUM
import com.tangem.features.feed.rwa.ui.state.RwaFeaturedUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.PaginationStatus
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The tab's Featured rail: the reserved `featured-assets` category rendered in full, in the order the
 * backend curates.
 *
 * [TokenMarketListConfig.Order.ByCategory] is what surfaces that curation — any other order re-sorts the
 * bucket and discards it. The rail is one batch ([GetMarketsTokenListFlowUseCase.BatchFlowType.Featured])
 * and never pages.
 *
 * Only [RwaListRequest.interval] is read from [request]; the rail deliberately ignores the tab's
 * category/sector filter, which scopes the list below it. A changed interval re-maps the loaded tokens
 * instead of reloading, because a token carries the change percent for every interval at once.
 */
internal class RwaFeaturedAssetsManager(
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    private val request: StateFlow<RwaListRequest>,
    private val appCurrency: StateFlow<AppCurrency>,
    private val onItemClick: (CryptoCurrency.RawID) -> Unit,
    private val modelScope: CoroutineScope,
) {

    private val actionsFlow = MutableSharedFlow<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()

    private val batchFlow = getMarketsTokenListFlowUseCase(
        batchingContext = TokenListBatchingContext(
            actionsFlow = actionsFlow,
            coroutineScope = modelScope,
        ),
        batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Featured,
    )

    val state: StateFlow<RwaFeaturedUM> = combine(
        flow = batchFlow.state,
        flow2 = request.map { it.interval }.distinctUntilChanged(),
    ) { batchState, interval ->
        val tokens = batchState.data.flatMap { it.data }

        when {
            tokens.isNotEmpty() -> RwaFeaturedUM.Content(
                assets = tokens.map { token -> toFeaturedAsset(token, interval) }.toImmutableList(),
            )
            batchState.status is PaginationStatus.InitialLoadingError -> RwaFeaturedUM.Empty
            batchState.status is PaginationStatus.EndOfPagination -> RwaFeaturedUM.Empty
            else -> RwaFeaturedUM.Loading
        }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = RwaFeaturedUM.Loading,
        )

    fun load(categoryId: String) {
        modelScope.launch {
            actionsFlow.emit(
                BatchAction.Reload(
                    requestParams = TokenMarketListConfig(
                        fiatPriceCurrency = appCurrency.value.code,
                        searchText = null,
                        priceChangeInterval = request.value.interval.toBatchRequestInterval(),
                        order = TokenMarketListConfig.Order.ByCategory,
                        categoryId = categoryId,
                    ),
                ),
            )
        }
    }

    /** The rail's tokens are absent from the list's batches whenever the curation and the list scope differ. */
    fun getTokenById(id: CryptoCurrency.RawID): TokenMarket? {
        return batchFlow.state.value.data
            .asSequence()
            .flatMap { it.data }
            .firstOrNull { it.id == id }
    }

    private fun toFeaturedAsset(token: TokenMarket, interval: RwaInterval): RwaFeaturedAssetUM {
        val changePercent = token.trendChangePercent(interval)

        return RwaFeaturedAssetUM(
            id = token.id.value,
            iconUrl = token.imageUrlLarge,
            name = stringReference(token.name),
            ticker = stringReference(token.symbol),
            price = stringReference(token.formatPrice(appCurrency.value)),
            priceChange = TangemPriceChange.State(
                value = stringReference(changePercent.format { percent() }),
                direction = changePercent.toPriceChangeDirection(),
            ),
            onClick = { onItemClick(token.id) },
        )
    }
}