package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketEventsBatchAction
import com.tangem.domain.polymarket.model.PolymarketEventsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveQuotesUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSportsStateUseCase
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.main.model.transformer.PolymarketFeedContentTransformer
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedFooterUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedScrollPosition
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList")
internal class PolymarketFeedController(
    private val scope: CoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
    private val userWalletId: UserWalletId,
    private val getEvents: GetPolymarketEventsBatchFlowUseCase,
    observeSports: ObservePolymarketSportsStateUseCase,
    observeQuotes: ObservePolymarketLiveQuotesUseCase,
    isPhaseTwoEnabled: Boolean,
    private val eventConverter: PolymarketEventUMConverter,
    private val onStaleData: () -> Unit,
    private val shouldCacheFeeds: Boolean = true,
) {

    val content: StateFlow<PolymarketMainUM.ContentUM>
        field = MutableStateFlow<PolymarketMainUM.ContentUM>(PolymarketMainUM.ContentUM.Loading)

    private val feeds = mutableMapOf<PolymarketEventsListConfig, Entry>()
    private val activeFeed = MutableStateFlow<PolymarketCategoryFeed?>(null)
    private var activeEntry: Entry? = null
    private val isResumed = MutableStateFlow(false)
    private val hasRefreshError = MutableStateFlow(false)
    private val feedRefreshJob = JobHolder()
    private val sports = PolymarketFeedSports(observeSports, isPhaseTwoEnabled)
    private val quotes = PolymarketFeedQuotes(observeQuotes, isPhaseTwoEnabled)

    init {
        activeFeed.filterNotNull().distinctUntilChangedBy { it.config }.flatMapLatest { selection ->
            channelFlow {
                val selectedFeeds = activeFeed.filterNotNull().filter { it.config == selection.config }
                val games = sports.observe(selectedFeeds, isResumed).stateIn(
                    scope = this,
                    started = SharingStarted.Eagerly,
                    initialValue = PolymarketSportsState(emptyMap()),
                )
                selectedFeeds.flatMapLatest { feed ->
                    combine(
                        flow = feed.batchFlow.state,
                        flow2 = games,
                        flow3 = quotes.observe(feed, games, isResumed),
                        flow4 = hasRefreshError,
                    ) { batch, state, prices, refreshError ->
                        val content = PolymarketFeedContentTransformer(
                            batchListState = batch,
                            eventUMConverter = eventConverter,
                            onReloadClick = ::reload,
                            onNextPageRetryClick = ::loadMore,
                            sports = state,
                            quotes = prices,
                        ).transformContent()
                        if (refreshError) refreshErrorContent(content) else content
                    }.map { feed to it }
                }.collect { send(it) }
            }
        }.flowOn(dispatchers.default)
            .onEach { (feed, state) -> if (activeFeed.value === feed) content.value = state }
            .launchIn(scope)
    }

    fun select(config: PolymarketEventsListConfig, reloadVisited: Boolean = false): PolymarketFeedScrollPosition? {
        activeFeed.value?.poller?.pause()
        feedRefreshJob.cancel()
        hasRefreshError.value = false
        if (!shouldCacheFeeds) activeEntry?.scope?.cancel()
        val visited = feeds[config]
        val entry = visited ?: createEntry(config)
        if (shouldCacheFeeds) feeds[config] = entry
        activeEntry = entry
        activeFeed.value = entry.feed
        if (visited == null || reloadVisited) reload()
        if (isResumed.value) entry.feed.poller.resume()
        return entry.feed.scrollPosition
    }

    fun reload() {
        val feed = activeFeed.value ?: return
        feedRefreshJob.cancel()
        hasRefreshError.value = false
        content.value = PolymarketMainUM.ContentUM.Loading
        scope.launch {
            feed.poller.onFeedReloaded(feed.config)
            feed.actionsFlow.emit(BatchAction.Reload(requestParams = feed.config))
        }
    }

    suspend fun refresh() {
        val job = scope.launch { replaceActiveFeed() }.saveIn(feedRefreshJob)
        try {
            job.join()
        } catch (e: CancellationException) {
            job.cancel()
            throw e
        }
    }

    private suspend fun replaceActiveFeed() {
        val previous = activeEntry ?: return
        val replacement = createEntry(previous.feed.config)
        hasRefreshError.value = false
        try {
            replacement.feed.poller.onFeedReloaded(replacement.feed.config)
            replacement.feed.actionsFlow.emit(BatchAction.Reload(requestParams = replacement.feed.config))
            val result = replacement.feed.batchFlow.state.first { !it.status.isAwaitingFirstPage() }
            if (result.status is PaginationStatus.InitialLoadingError) {
                val previousState = previous.feed.batchFlow.state.value
                hasRefreshError.value = !previousState.status.isAwaitingFirstPage()
                return
            }
            previous.feed.poller.pause()
            previous.scope.cancel()
            replacement.feed.scrollPosition = previous.feed.scrollPosition
            val visibleIds = previous.feed.visibleEventIds.value
            replacement.feed.visibleEventIds.value = visibleIds
            replacement.feed.poller.setVisibleEventIds(visibleIds)
            activeEntry = replacement
            if (shouldCacheFeeds) feeds[replacement.feed.config] = replacement
            activeFeed.value = replacement.feed
            if (isResumed.value) replacement.feed.poller.resume()
        } finally {
            if (activeEntry !== replacement) replacement.scope.cancel()
        }
    }

    private fun refreshErrorContent(content: PolymarketMainUM.ContentUM): PolymarketMainUM.ContentUM {
        val retry: () -> Unit = { scope.launch { refresh() } }
        return when (content) {
            is PolymarketMainUM.ContentUM.Content -> if (content.footer is PolymarketFeedFooterUM.None) {
                content.copy(footer = PolymarketFeedFooterUM.Error(onRetryClick = retry))
            } else {
                content
            }
            is PolymarketMainUM.ContentUM.Loading -> content
            is PolymarketMainUM.ContentUM.Empty,
            is PolymarketMainUM.ContentUM.Error,
            -> PolymarketMainUM.ContentUM.Error(onReloadClick = retry)
        }
    }

    fun loadMore() {
        val feed = activeFeed.value ?: return
        scope.launch {
            val status = feed.batchFlow.state.value.status as? PaginationStatus.Paginating ?: return@launch
            val lastResult = status.lastResult
            if (lastResult !is BatchFetchResult.Success || !lastResult.isLast) {
                hasRefreshError.value = false
                feed.actionsFlow.emit(BatchAction.LoadMore())
            }
        }
    }

    fun setVisibleEvents(eventIds: Set<String>) {
        val feed = activeFeed.value ?: return
        feed.visibleEventIds.value = eventIds
        feed.poller.setVisibleEventIds(eventIds)
    }

    fun onScrollIdle(index: Int, offset: Int) {
        val feed = activeFeed.value ?: return
        feed.scrollPosition = PolymarketFeedScrollPosition(index = index, offset = offset)
        feed.poller.onScrollIdle()
    }

    fun resume() {
        isResumed.value = true
        activeFeed.value?.poller?.resume()
    }

    fun pause() {
        isResumed.value = false
        activeFeed.value?.poller?.pause()
    }

    private fun createEntry(config: PolymarketEventsListConfig): Entry {
        val feedScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext.job))
        val actions = MutableSharedFlow<PolymarketEventsBatchAction>(replay = 1)
        val batches = getEvents(
            userWalletId = userWalletId,
            context = PolymarketEventsBatchingContext(actionsFlow = actions, coroutineScope = feedScope),
        )
        val poller = PolymarketFeedPoller(
            scope = feedScope,
            batchFlow = batches,
            actionsFlow = actions,
            onStaleData = onStaleData,
        )
        poller.start()
        return Entry(
            feed = PolymarketCategoryFeed(config = config, actionsFlow = actions, batchFlow = batches, poller = poller),
            scope = feedScope,
        )
    }

    private fun PaginationStatus<*>.isAwaitingFirstPage(): Boolean =
        this is PaginationStatus.None || this is PaginationStatus.InitialLoading

    private data class Entry(val feed: PolymarketCategoryFeed, val scope: CoroutineScope)
}