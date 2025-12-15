package com.tangem.features.feed.model.feed

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.news.usecase.FetchTrendingNewsUseCase
import com.tangem.domain.news.usecase.ManageTrendingNewsUseCase
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.feed.state.*
import com.tangem.features.feed.ui.market.state.SortByTypeUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import javax.inject.Inject

@Stable
@ModelScoped
internal class FeedComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val fetchTrendingNewsUseCase: FetchTrendingNewsUseCase,
    private val manageTrendingNewsUseCase: ManageTrendingNewsUseCase,
) : Model() {

    private val _state = MutableStateFlow(initialState())
    val state = _state.asStateFlow()

    private val searchBarStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        SearchBarStateFactory(
            currentStateProvider = Provider { _state.value },
            onStateUpdate = { newState -> _state.update { newState } },
        )
    }

    private val trendingNewsStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        TrendingNewsStateFactory(
            currentStateProvider = Provider { _state.value },
            onStateUpdate = { newState -> _state.update { newState } },
        )
    }

    init {
        modelScope.launch(dispatchers.default) {
            fetchTrendingNewsUseCase()
            subscribeOnTrendingNews()
        }
        _state.update { feedListUM ->
            feedListUM.copy(
                searchBar = _state.value.searchBar.copy(onQueryChange = searchBarStateFactory::onSearchQueryChange),
            )
        }
    }

    private suspend fun subscribeOnTrendingNews() {
        manageTrendingNewsUseCase().collect { articles ->
            trendingNewsStateFactory.updateTrendingNewsState(articles)
        }
    }

    private fun initialState(): FeedListUM {
        return FeedListUM(
            currentDate = getCurrentDate(),
            searchBar = SearchBarUM(
                placeholderText = resourceReference(R.string.markets_search_header_title),
                query = "",
                onQueryChange = {},
                isActive = false,
                onActiveChange = { },
            ),
            feedListCallbacks = FeedListCallbacks(
                onSearchClick = {},
                onMarketOpenClick = {},
                onArticleClick = {},
                onOpenAllNews = {},
                onMarketItemClick = {},
                onSortTypeClick = {},
            ),
            news = NewsUM.Loading,
            trendingArticle = null,
            marketChartConfig = MarketChartConfig(
                marketCharts = buildMap {
                    SortByTypeUM.entries.forEach {
                        put(it, MarketChartUM.Loading)
                    } // TODO in [REDACTED_TASK_KEY] add correct sorting
                }.toPersistentHashMap(),
                currentSortByType = SortByTypeUM.TopGainers,
            ),
        )
    }

    private fun getCurrentDate(): String {
        val localDate = DateTime(DateTime.now(), DateTimeZone.getDefault())
        return DateTimeFormatters.formatDate(formatter = DateTimeFormatters.dateDMMM, date = localDate)
    }
}