package com.tangem.features.feed.search.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.feed.search.model.RecentFeedSearchItem
import com.tangem.domain.feed.search.usecase.ClearFeedSearchHistoryUseCase
import com.tangem.domain.feed.search.usecase.GetRecentFeedSearchItemsUseCase
import com.tangem.domain.feed.search.usecase.GetRecentFeedSearchQueriesUseCase
import com.tangem.domain.feed.search.usecase.RemoveFeedSearchQueryUseCase
import com.tangem.domain.feed.search.usecase.SaveFeedSearchQueryUseCase
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.search.FeedSearchBarController
import com.tangem.features.feed.search.model.analytics.FeedSearchAnalyticsEvent
import com.tangem.features.feed.search.ui.state.FeedSearchUM
import com.tangem.features.feed.search.ui.state.RecentUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
@ModelScoped
internal class FeedSearchModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val searchBarController: FeedSearchBarController,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
    private val removeFeedSearchQueryUseCase: RemoveFeedSearchQueryUseCase,
    private val clearFeedSearchHistoryUseCase: ClearFeedSearchHistoryUseCase,
    private val saveFeedSearchQueryUseCase: SaveFeedSearchQueryUseCase,
    getRecentFeedSearchItemsUseCase: GetRecentFeedSearchItemsUseCase,
    getRecentFeedSearchQueriesUseCase: GetRecentFeedSearchQueriesUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : Model() {

    private val params = paramsContainer.require<FeedRoute.Search>()

    // the query is typed into the host's search bar, not into this screen. Eagerly, because the tab
    // pages are constructed before anything composes and must already see the current query
    val query: StateFlow<String> = searchBarController.state
        .map { it.query }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = searchBarController.state.value.query,
        )

    private val appCurrency = getSelectedAppCurrencyUseCase.invokeOrDefault()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppCurrency.Default,
        )

    val uiState: StateFlow<FeedSearchUM> = combine(
        flow = query,
        flow2 = getRecentFeedSearchItemsUseCase(),
        flow3 = getRecentFeedSearchQueriesUseCase(),
    ) { query, items, queries ->
        FeedSearchUM(
            source = params.source,
            query = query,
            recent = toRecentUM(items = items, queries = queries),
        )
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = FeedSearchUM(source = params.source, query = query.value),
    )

    init {
        analyticsEventHandler.send(FeedSearchAnalyticsEvent.SearchScreenOpened(params.source))

        // once per screen, on the first typed character: the bar opens empty, so drop its initial value
        query.drop(1)
            .filter(String::isNotEmpty)
            .take(1)
            .onEach { analyticsEventHandler.send(FeedSearchAnalyticsEvent.SearchStarted()) }
            .launchIn(modelScope)

        searchBarController.submits
            .onEach { saveFeedSearchQueryUseCase(query.value) }
            .launchIn(modelScope)
    }

    private fun toRecentUM(items: List<RecentFeedSearchItem>, queries: List<String>): RecentUM? {
        if (items.isEmpty() && queries.isEmpty()) return null

        return RecentUM(
            items = items.map(::toItemUM).toImmutableList(),
            queries = queries.take(DISPLAYED_QUERIES_COUNT).map(::toQueryUM).toImmutableList(),
            onClearClick = ::onClearClick,
        )
    }

    private fun toItemUM(item: RecentFeedSearchItem): RecentUM.ItemUM = when (item) {
        is RecentFeedSearchItem.MarketToken -> RecentUM.ItemUM(
            id = item.id,
            title = item.token.name,
            icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = item.token.imageUrl)),
            onClick = { openMarketToken(item) },
        )
    }

    private fun toQueryUM(query: String): RecentUM.QueryUM = RecentUM.QueryUM(
        text = query,
        onClick = { onQueryClick(query) },
        onRemoveClick = { modelScope.launch { removeFeedSearchQueryUseCase(query) } },
    )

    private fun onQueryClick(query: String) {
        analyticsEventHandler.send(FeedSearchAnalyticsEvent.HintClicked(query))
        searchBarController.onQueryChange(query)
    }

    private fun onClearClick() {
        analyticsEventHandler.send(FeedSearchAnalyticsEvent.ButtonClearHistoryClick())
        modelScope.launch { clearFeedSearchHistoryUseCase() }
    }

    private fun openMarketToken(item: RecentFeedSearchItem.MarketToken) {
        analyticsEventHandler.send(FeedSearchAnalyticsEvent.RecentItemClicked(item.token.symbol))
        router.push(
            route = FeedRoute.MarketsTokenDetails(
                token = item.token,
                appCurrency = appCurrency.value,
                shouldShowPortfolio = true,
                analyticsParams = FeedRoute.MarketsTokenDetails.AnalyticsParams(
                    blockchain = null,
                    source = params.source,
                ),
            ),
        )
    }

    private companion object {

        const val DISPLAYED_QUERIES_COUNT = 3
    }
}