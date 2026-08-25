package com.tangem.features.polymarket.impl.main.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketEventsBatchAction
import com.tangem.domain.polymarket.model.PolymarketEventsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.usecase.GetPolymarketCategoriesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketCategoryTabUMConverter
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.main.model.transformer.PolymarketFeedContentTransformer
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Model of the Discovery feed screen.
 *
 * Loads the category tabs once, then serves the events of the selected category page by page. Categories are a
 * filtering convenience rather than content: when they fail, the tabs stay hidden and the feed runs unfiltered
 * instead of failing with them.
 */
@ModelScoped
internal class PolymarketMainModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getPolymarketEventsBatchFlowUseCase: GetPolymarketEventsBatchFlowUseCase,
    private val getPolymarketCategoriesUseCase: GetPolymarketCategoriesUseCase,
) : Model() {

    private val params = paramsContainer.require<PolymarketMainParams>()

    val uiState: StateFlow<PolymarketMainUM>
        field = MutableStateFlow(
            PolymarketMainUM(
                categories = persistentListOf(),
                content = PolymarketMainUM.ContentUM.Loading,
            ),
        )

    private val eventUMConverter = PolymarketEventUMConverter(
        onEventClick = ::onEventClick,
        onOutcomeClick = ::onOutcomeClick,
    )

    private val categoryTabConverter = PolymarketCategoryTabUMConverter(onCategoryClick = ::onCategorySelected)

    // Replays the latest action: the first Reload is dispatched while the pagination is still subscribing, and a
    // shared flow without a replay cache drops what it cannot deliver yet.
    private val actionsFlow = MutableSharedFlow<PolymarketEventsBatchAction>(replay = 1)

    private val eventsBatchFlow = getPolymarketEventsBatchFlowUseCase(
        context = PolymarketEventsBatchingContext(actionsFlow = actionsFlow, coroutineScope = modelScope),
    )

    private var categories: List<PolymarketCategory> = emptyList()
    private var selectedCategoryId: Int? = null

    private val categoriesJob = JobHolder()

    private val feedPoller = PolymarketFeedPoller(
        scope = modelScope,
        batchFlow = eventsBatchFlow,
        actionsFlow = actionsFlow,
        onStaleData = ::reportStaleData,
    )

    init {
        observeEvents()
        loadCategories()
        feedPoller.start()
    }

    fun onBackClick() {
        router.pop()
    }

    fun onSearchClick() {
        router.push(PolymarketRoute.Search(userWalletId = params.userWalletId))
    }

    /**
     * Requests the next page once the feed is scrolled close enough to its end. Ignored while a page is already
     * on its way, and while the feed is empty or broken — those are driven by [reload] instead.
     */
    fun onLoadMore() {
        modelScope.launch {
            when (val status = eventsBatchFlow.state.value.status) {
                is PaginationStatus.Paginating -> {
                    val lastResult = status.lastResult
                    // A failed page is worth another attempt as the user keeps scrolling; a last one is not.
                    val canLoadMore = lastResult !is BatchFetchResult.Success || !lastResult.last
                    if (canLoadMore) actionsFlow.emit(BatchAction.LoadMore())
                }
                else -> Unit
            }
        }
    }

    /** The cards currently on screen: the poller refreshes the pages they come from and no others. */
    fun onVisibleEventsChange(eventIds: Set<String>) {
        feedPoller.setVisibleEventIds(eventIds)
    }

    fun onScrollIdle() {
        feedPoller.onScrollIdle()
    }

    fun onResume() {
        feedPoller.resume()
    }

    fun onPause() {
        feedPoller.pause()
    }

    /** Categories first: without them the feed does not know which category to ask for. */
    private fun loadCategories() {
        modelScope.launch {
            val result = withContext(dispatchers.default) { getPolymarketCategoriesUseCase() }
            val loadedCategories = result.getOrNull().orEmpty()

            categories = loadedCategories
            selectedCategoryId = loadedCategories.firstOrNull()?.id
            uiState.update { it.copy(categories = buildTabs()) }

            reloadEvents()
        }.saveIn(categoriesJob)
    }

    private fun onCategorySelected(categoryId: Int) {
        if (categoryId == selectedCategoryId) return
        selectedCategoryId = categoryId
        // Highlight the tapped tab immediately; the feed catches up once its first page arrives.
        uiState.update { it.copy(categories = buildTabs()) }
        reloadEvents()
    }

    /** Retries the feed and, when the categories were lost too, the tabs along with it. */
    private fun reload() {
        if (categories.isEmpty()) loadCategories() else reloadEvents()
    }

    private fun reloadEvents() {
        modelScope.launch {
            val config = PolymarketEventsListConfig(category = selectedCategoryId)
            feedPoller.onFeedReloaded(config)
            actionsFlow.emit(BatchAction.Reload(requestParams = config))
        }
    }

    private fun observeEvents() {
        eventsBatchFlow.state
            .onEach { batchState ->
                val transformer = PolymarketFeedContentTransformer(
                    batchListState = batchState,
                    eventUMConverter = eventUMConverter,
                    onReloadClick = ::reload,
                )
                uiState.update(transformer::transform)
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun buildTabs() = categoryTabConverter.convert(
        value = PolymarketCategoryTabUMConverter.Input(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
        ),
    )

    private fun reportStaleData() {
        messageSender.send(SnackbarMessage(message = resourceReference(R.string.prediction_main_data_outdated)))
    }

    private fun onEventClick(eventId: String) {
        router.push(PolymarketRoute.EventDetails(eventId = eventId, userWalletId = params.userWalletId))
    }

    private fun onOutcomeClick(eventId: String, marketId: String, assetId: String) {
        router.push(
            PolymarketRoute.EventDetails(
                eventId = eventId,
                userWalletId = params.userWalletId,
                marketId = marketId,
                assetId = assetId,
            ),
        )
    }
}