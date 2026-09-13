package com.tangem.features.feed.crypto.model

import arrow.core.getOrElse
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.toSerializableParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.crypto.model.list.MarketPulseBatchFlowManager
import com.tangem.features.feed.crypto.model.list.MarketPulseCategory
import com.tangem.features.feed.crypto.model.list.MarketPulseInterval
import com.tangem.features.feed.crypto.ui.state.CryptoFeedTabUM
import com.tangem.features.feed.crypto.ui.state.MarketIndexCardUM
import com.tangem.features.feed.crypto.ui.state.MarketPulseCategoryUM
import com.tangem.features.feed.crypto.ui.state.MarketPulseListUM
import com.tangem.features.feed.crypto.ui.state.MarketPulseUM
import com.tangem.features.feed.crypto.ui.state.TotalMarketCapUM
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val UPDATE_QUOTES_TIMER_MILLIS = 60_000L

@ModelScoped
internal class CryptoFeedTabModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val router: Router,
) : Model() {

    /** Expanded/collapsed state of the hosting shtorka — quotes refresh only while expanded. */
    val containerBottomSheetState = MutableStateFlow(BottomSheetState.COLLAPSED)
    val isVisibleOnScreen = MutableStateFlow(false)

    private val updateQuotesJob = JobHolder()

    private val currentAppCurrency = getSelectedAppCurrencyUseCase().map { maybeAppCurrency ->
        maybeAppCurrency.getOrElse { AppCurrency.Default }
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppCurrency.Default,
    )

    private val selectedInterval = MutableStateFlow(MarketPulseInterval.H24)
    private val selectedCategory = MutableStateFlow(MarketPulseCategory.MarketCap)
    private val visibleItemIds = MutableStateFlow<List<CryptoCurrency.RawID>>(emptyList())

    private val listManager = MarketPulseBatchFlowManager(
        getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
        currentTrendInterval = { selectedInterval.value },
        currentAppCurrency = { currentAppCurrency.value },
        currentCategory = { selectedCategory.value },
        onItemClick = ::openTokenDetails,
        modelScope = modelScope,
        dispatchers = dispatchers,
    )

    val uiState: StateFlow<CryptoFeedTabUM>
        field = MutableStateFlow(initialState())

    init {
        combine(
            flow = listManager.uiItems,
            flow2 = listManager.isInInitialLoadingErrorState,
        ) { items, isInErrorState ->
            when {
                isInErrorState -> MarketPulseListUM.Error(onRetry = { listManager.reload() })
                items.isEmpty() -> MarketPulseListUM.Loading
                else -> MarketPulseListUM.Content(items = items, loadMore = listManager::loadMore)
            }
        }
            .distinctUntilChanged()
            .onEach { list -> updateMarketPulse { it.copy(list = list) } }
            .launchIn(modelScope)

        // reload the list when the user's currency changes
        currentAppCurrency.drop(1)
            .onEach { listManager.reload() }
            .launchIn(modelScope)

        // load charts for a freshly loaded batch and keep quotes fresh
        listManager.onLastBatchLoadedSuccess.onEach { batchKey ->
            listManager.loadCharts(setOf(batchKey), selectedInterval.value)
            modelScope.loadQuotesWithTimer(timeMillis = UPDATE_QUOTES_TIMER_MILLIS)
        }.launchIn(modelScope)

        // interval switch: rating order only re-renders + fetches missing charts, others re-sort
        selectedInterval.drop(1).onEach { interval ->
            updateMarketPulse { it.copy(interval = intervalFilterItem(interval)) }
            if (selectedCategory.value == MarketPulseCategory.MarketCap) {
                listManager.updateUIWithSameState()
                listManager.loadCharts(listManager.getBatchKeysByItemIds(visibleItemIds.value), interval)
            } else {
                listManager.reload()
            }
        }.launchIn(modelScope)

        // category = sort order, so a switch always reloads
        selectedCategory.drop(1)
            .onEach { listManager.reload() }
            .launchIn(modelScope)

        // fetch charts for the batches that become visible while scrolling
        visibleItemIds
            .mapNotNull { ids -> ids.takeIf { it.isNotEmpty() }?.let(listManager::getBatchKeysByItemIds) }
            .distinctUntilChanged()
            .onEach { visibleBatchKeys -> listManager.loadCharts(visibleBatchKeys, selectedInterval.value) }
            .launchIn(modelScope)

        listManager.reload()
    }

    private fun initialState(): CryptoFeedTabUM {
        return CryptoFeedTabUM(
            totalMarketCap = stubTotalMarketCap(),
            marketPulse = MarketPulseUM(
                title = stringReference("Market Pulse"),
                interval = intervalFilterItem(selectedInterval.value),
                categories = MarketPulseCategory.entries
                    .map { MarketPulseCategoryUM(id = it.id, title = stringReference(it.categoryTitle())) }
                    .toImmutableList(),
                selectedCategoryIndex = 0,
                onCategorySelect = ::selectCategory,
                list = MarketPulseListUM.Loading,
                onVisibleItemsChange = { ids -> visibleItemIds.value = ids.map(CryptoCurrency::RawID) },
            ),
        )
    }

    private fun selectCategory(index: Int) {
        val category = MarketPulseCategory.entries.getOrNull(index) ?: return
        updateMarketPulse { it.copy(selectedCategoryIndex = index) }
        selectedCategory.value = category
    }

    // TODO: [TWI-1608] a proper interval picker; clicking cycles 24h → 7d → 1m for now
    private fun cycleInterval() {
        val entries = MarketPulseInterval.entries
        selectedInterval.update { current -> entries[(current.ordinal + 1) % entries.size] }
    }

    private fun intervalFilterItem(interval: MarketPulseInterval): TangemFilterItemUM {
        val label = when (interval) {
            MarketPulseInterval.H24 -> "24 hours"
            MarketPulseInterval.D7 -> "7 days"
            MarketPulseInterval.M1 -> "1 month"
        }
        return TangemFilterItemUM.Inactive(
            id = "interval",
            label = stringReference(label),
            onClick = ::cycleInterval,
        )
    }

    private fun MarketPulseCategory.categoryTitle(): String {
        return when (this) {
            MarketPulseCategory.MarketCap -> "Market cap"
            MarketPulseCategory.TopGainers -> "Top gainers"
            MarketPulseCategory.TopLosers -> "Top losers"
            MarketPulseCategory.ExperiencedBuyers -> "Experienced buyers"
            MarketPulseCategory.Trending -> "Trending"
        }
    }

    private inline fun updateMarketPulse(block: (MarketPulseUM) -> MarketPulseUM) {
        uiState.update { state -> state.copy(marketPulse = block(state.marketPulse)) }
    }

    private fun openTokenDetails(id: CryptoCurrency.RawID) {
        val token = listManager.getTokenById(id) ?: return
        router.push(
            route = FeedRoute.MarketsTokenDetails(
                token = token.toSerializableParam(),
                appCurrency = currentAppCurrency.value,
                shouldShowPortfolio = true,
                analyticsParams = FeedRoute.MarketsTokenDetails.AnalyticsParams(
                    blockchain = null,
                    source = AnalyticsParam.ScreensSources.Main.value,
                ),
            ),
        )
    }

    private fun CoroutineScope.loadQuotesWithTimer(timeMillis: Long) {
        launch {
            while (true) {
                delay(timeMillis)
                // update quotes only when the shtorka is expanded and the tab is visible
                containerBottomSheetState.first { it == BottomSheetState.EXPANDED }
                isVisibleOnScreen.first { it }
                listManager.updateQuotes()
            }
        }.saveIn(updateQuotesJob)
    }

    // TODO: [TWI-1608] stub mirroring the design mock until real market metrics are wired
    @Suppress("MagicNumber")
    private fun stubTotalMarketCap(): TotalMarketCapUM {
        return TotalMarketCapUM(
            title = stringReference("Total market cap"),
            value = stringReference("\$2.44 T"),
            change = TangemPriceChange.State(
                value = stringReference("1.18%"),
                direction = TangemPriceChange.Direction.Up,
            ),
            indexes = persistentListOf(
                MarketIndexCardUM(
                    id = "fear_greed",
                    title = stringReference("Fear & greed"),
                    statusLabel = stringReference("Extreme Fear"),
                    isGrowth = false,
                    value = stringReference("13"),
                    progress = 0.13f,
                ),
                MarketIndexCardUM(
                    id = "altcoin_index",
                    title = stringReference("Altcoin index"),
                    statusLabel = stringReference("Altcoin Growth"),
                    isGrowth = true,
                    value = stringReference("56"),
                    progress = 0.56f,
                ),
            ),
        )
    }
}