package com.tangem.features.polymarket.impl.category.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.res.R
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.polymarket.model.PolymarketCategoryFilter
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveQuotesUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSportsStateUseCase
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.polymarket.impl.category.PolymarketCategoryComponent
import com.tangem.features.polymarket.impl.category.ui.state.PolymarketCategoryUM
import com.tangem.features.polymarket.impl.main.model.PolymarketStaleDataReporter
import com.tangem.features.polymarket.impl.common.region.PredictionRegionAccess
import com.tangem.features.polymarket.impl.common.region.regionRestrictionsMessage
import com.tangem.features.polymarket.impl.main.model.PolymarketFeedController
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketCategoryFilterUMConverter
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketCategoryTabUMConverter
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class PolymarketCategoryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val regionAccess: PredictionRegionAccess,
    getEvents: GetPolymarketEventsBatchFlowUseCase,
    observeSports: ObservePolymarketSportsStateUseCase,
    observeQuotes: ObservePolymarketLiveQuotesUseCase,
    featureToggles: PolymarketFeatureToggles,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<PolymarketCategoryComponent.Params>()
    private val isEnabled = featureToggles.isPolymarketPhaseTwoEnabled
    private var selectedSubcategoryId: Int? = null
    private var selectedFilter = availableFilters().firstOrNull()
    private val tabConverter = PolymarketCategoryTabUMConverter(::selectSubcategory)
    private val screenRefreshJob = JobHolder()
    private val staleReporter = PolymarketStaleDataReporter(
        onReport = { messageSender.send(SnackbarMessage(resourceReference(R.string.prediction_main_data_outdated))) },
    )

    val uiState: StateFlow<PolymarketCategoryUM>
        field = MutableStateFlow(
            PolymarketCategoryUM(
                title = stringReference(params.category.label),
                selection = selection(),
                pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = { refresh() }),
            ),
        )

    private val feed = PolymarketFeedController(
        scope = modelScope,
        dispatchers = dispatchers,
        userWalletId = params.userWalletId,
        getEvents = getEvents,
        observeSports = observeSports,
        observeQuotes = observeQuotes,
        isPhaseTwoEnabled = isEnabled,
        eventConverter = PolymarketEventUMConverter(::openEvent, ::openOutcome, isPhaseTwoEnabled = isEnabled),
        onStaleData = staleReporter::report,
        shouldCacheFeeds = false,
    )

    init {
        if (isEnabled) {
            feed.content.onEach { content -> uiState.update { it.copy(content = content) } }.launchIn(modelScope)
            selectFeed()
        } else {
            modelScope.launch { router.pop() }
        }
    }

    fun onBack() = router.pop()

    fun onResume() {
        if (isEnabled) feed.resume()
    }

    fun onPause() = feed.pause()

    fun onLoadMore() = feed.loadMore()

    fun onVisibleEventsChange(ids: Set<String>) = feed.setVisibleEvents(ids)

    fun onScrollIdle(index: Int, offset: Int) = feed.onScrollIdle(index, offset)

    private fun selectedCategory() = params.category.subcategories.firstOrNull { it.id == selectedSubcategoryId }
        ?: params.category

    private fun availableFilters() = PolymarketCategoryFilterUMConverter.available(selectedCategory())

    private fun selection() = PolymarketEventsListConfig(selectedCategory().id, selectedFilter)

    private fun selectSubcategory(id: Int) {
        if (!isEnabled) return
        if (id != params.category.id && params.category.subcategories.none { it.id == id }) return
        val selected = id.takeUnless { it == params.category.id }
        if (selected == selectedSubcategoryId) return
        selectedSubcategoryId = selected
        selectedFilter = availableFilters().firstOrNull()
        selectFeed()
    }

    private fun selectFilter(filter: PolymarketCategoryFilter) {
        if (!isEnabled || filter == selectedFilter || filter !in availableFilters()) return
        selectedFilter = filter
        selectFeed()
    }

    private fun selectFeed() {
        screenRefreshJob.cancel()
        val selection = selection()
        uiState.update { state ->
            state.copy(
                selection = selection,
                content = PolymarketMainUM.ContentUM.Loading,
                subcategories = tabConverter.subcategories(params.category, selectedSubcategoryId),
                filters = PolymarketCategoryFilterUMConverter.convert(
                    category = selectedCategory(),
                    selected = selectedFilter,
                    onSelect = ::selectFilter,
                ),
                pullToRefreshConfig = state.pullToRefreshConfig.copy(isRefreshing = false),
            )
        }
        feed.select(selection)
    }

    private fun refresh() {
        if (!isEnabled || uiState.value.pullToRefreshConfig.isRefreshing) return
        setRefreshing(true)
        modelScope.launch {
            try {
                coroutineScope {
                    launch { regionAccess.refresh() }
                    feed.refresh()
                }
            } finally {
                setRefreshing(false)
            }
        }.saveIn(screenRefreshJob)
    }

    private fun setRefreshing(value: Boolean) {
        uiState.update { it.copy(pullToRefreshConfig = it.pullToRefreshConfig.copy(isRefreshing = value)) }
    }

    private fun openEvent(eventId: String) {
        router.push(
            PolymarketRoute.EventDetails(
                eventId = eventId,
                userWalletId = params.userWalletId,
                categoryLabel = selectedCategory().label,
            ),
        )
    }

    private fun openOutcome(eventId: String, marketId: String, assetId: String) {
        if (regionAccess.isRestricted) return messageSender.send(regionRestrictionsMessage())
        router.push(
            PolymarketRoute.PlacePrediction(
                userWalletId = params.userWalletId,
                eventId = eventId,
                marketId = marketId,
                assetId = assetId,
            ),
        )
    }
}