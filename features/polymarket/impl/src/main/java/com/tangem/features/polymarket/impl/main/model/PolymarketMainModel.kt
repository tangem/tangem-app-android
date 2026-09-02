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
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.polymarket.usecase.GetPolymarketCategoriesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketCategoryTabUMConverter
import com.tangem.features.polymarket.impl.main.model.converter.PredictionBalanceUMConverter
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.main.model.transformer.PolymarketFeedContentTransformer
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedScrollPosition
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@OptIn(ExperimentalCoroutinesApi::class)
@ModelScoped
@Suppress("LongParameterList")
internal class PolymarketMainModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getPolymarketEventsBatchFlowUseCase: GetPolymarketEventsBatchFlowUseCase,
    private val getPolymarketCategoriesUseCase: GetPolymarketCategoriesUseCase,
    private val predictionAccountStatusSupplier: PredictionAccountStatusSupplier,
) : Model() {

    private val params = paramsContainer.require<PolymarketMainParams>()

    val uiState: StateFlow<PolymarketMainUM>
        field = MutableStateFlow(
            PolymarketMainUM(
                balance = null,
                categories = persistentListOf(),
                content = PolymarketMainUM.ContentUM.Loading,
            ),
        )

    private val eventUMConverter = PolymarketEventUMConverter(
        onEventClick = ::onEventClick,
        onOutcomeClick = ::onOutcomeClick,
    )

    private val categoryTabConverter = PolymarketCategoryTabUMConverter(onCategoryClick = ::onCategorySelected)

    private val balanceConverter = PredictionBalanceUMConverter(
        onAddFundsClick = ::onAddFundsClick,
        onWithdrawClick = ::onWithdrawClick,
    )

    init {
        predictionAccountStatusSupplier(userWalletId = params.userWalletId)
            .onEach { status -> uiState.update { it.copy(balance = balanceConverter.convert(status)) } }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    // Kept for as long as the screen lives: coming back to a tab must not cost a reload.
    private val feeds = mutableMapOf<Int?, PolymarketCategoryFeed>()

    private val activeFeed = MutableStateFlow<PolymarketCategoryFeed?>(value = null)

    private var categories: List<PolymarketCategory> = emptyList()
    private var selectedCategoryId: Int? = null
    private var isScreenResumed = false

    private val categoriesJob = JobHolder()

    init {
        observeEvents()
        loadCategories()
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
        val feed = activeFeed.value ?: return

        modelScope.launch {
            when (val status = feed.batchFlow.state.value.status) {
                is PaginationStatus.Paginating -> {
                    val lastResult = status.lastResult
                    // A failed page is worth another attempt as the user keeps scrolling; a last one is not.
                    val canLoadMore = lastResult !is BatchFetchResult.Success || !lastResult.last
                    if (canLoadMore) feed.actionsFlow.emit(BatchAction.LoadMore())
                }
                else -> Unit
            }
        }
    }

    fun onVisibleEventsChange(eventIds: Set<String>) {
        activeFeed.value?.poller?.setVisibleEventIds(eventIds)
    }

    fun onScrollIdle(index: Int, offset: Int) {
        val feed = activeFeed.value ?: return

        feed.scrollPosition = PolymarketFeedScrollPosition(index = index, offset = offset)
        feed.poller.onScrollIdle()
    }

    fun onScrollPositionRestore() {
        uiState.update { it.copy(pendingScrollPosition = null) }
    }

    fun onResume() {
        isScreenResumed = true
        activeFeed.value?.poller?.resume()
    }

    fun onPause() {
        isScreenResumed = false
        activeFeed.value?.poller?.pause()
    }

    /** Categories first: without them the feed does not know which category to ask for. */
    private fun loadCategories() {
        modelScope.launch {
            val result = withContext(dispatchers.default) { getPolymarketCategoriesUseCase() }
            val loadedCategories = result.getOrNull().orEmpty()

            categories = loadedCategories
            selectedCategoryId = loadedCategories.firstOrNull()?.id
            uiState.update { it.copy(categories = buildTabs()) }

            selectFeed(categoryId = selectedCategoryId, reloadVisited = true)
        }.saveIn(categoriesJob)
    }

    private fun onCategorySelected(categoryId: Int) {
        if (categoryId == selectedCategoryId) return
        selectedCategoryId = categoryId
        // Highlight the tapped tab immediately; the feed catches up once its own pages are on screen.
        uiState.update { it.copy(categories = buildTabs()) }
        selectFeed(categoryId = categoryId)
    }

    /**
     * @param reloadVisited request the first page even for a feed that was visited — the retry path arrives here
     *  with the very feed that failed, and that is what it is asking for.
     */
    private fun selectFeed(categoryId: Int?, reloadVisited: Boolean = false) {
        activeFeed.value?.poller?.pause()

        val visited = feeds[categoryId]
        val feed = visited ?: createFeed(config = PolymarketEventsListConfig(category = categoryId))
            .also { feeds[categoryId] = it }

        activeFeed.value = feed
        uiState.update { it.copy(pendingScrollPosition = feed.scrollPosition) }

        if (visited == null || reloadVisited) reloadEvents(feed = feed)
        if (isScreenResumed) feed.poller.resume()
    }

    private fun createFeed(config: PolymarketEventsListConfig): PolymarketCategoryFeed {
        // Replays the latest action: the first Reload is dispatched while the pagination is still subscribing, and
        // a shared flow without a replay cache drops what it cannot deliver yet.
        val actionsFlow = MutableSharedFlow<PolymarketEventsBatchAction>(replay = 1)
        val batchFlow = getPolymarketEventsBatchFlowUseCase(
            context = PolymarketEventsBatchingContext(actionsFlow = actionsFlow, coroutineScope = modelScope),
        )
        val poller = PolymarketFeedPoller(
            scope = modelScope,
            batchFlow = batchFlow,
            actionsFlow = actionsFlow,
            onStaleData = ::reportStaleData,
        )
        poller.start()

        return PolymarketCategoryFeed(
            config = config,
            actionsFlow = actionsFlow,
            batchFlow = batchFlow,
            poller = poller,
        )
    }

    /** Retries the feed and, when the categories were lost too, the tabs along with it. */
    private fun reload() {
        if (categories.isEmpty()) loadCategories() else activeFeed.value?.let(::reloadEvents)
    }

    private fun reloadEvents(feed: PolymarketCategoryFeed) {
        modelScope.launch {
            feed.poller.onFeedReloaded(feed.config)
            feed.actionsFlow.emit(BatchAction.Reload(requestParams = feed.config))
        }
    }

    private fun observeEvents() {
        activeFeed
            .filterNotNull()
            .flatMapLatest { feed -> feed.batchFlow.state }
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
        router.push(
            PolymarketRoute.EventDetails(eventId = eventId, userWalletId = params.userWalletId),
        )
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

    private fun onAddFundsClick() {
        // Opens the Add funds sheet, which [REDACTED_TASK_KEY] adds.
    }

    private fun onWithdrawClick() {
        // Opens the withdrawal flow, which [REDACTED_TASK_KEY] adds.
    }
}