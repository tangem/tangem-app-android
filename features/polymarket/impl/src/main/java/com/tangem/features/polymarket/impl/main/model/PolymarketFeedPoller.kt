package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsBatchAction
import com.tangem.domain.polymarket.model.PolymarketEventsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketEventsBatchListState
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketEventsUpdateRequest
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.exception.OperationWIthTheSameIdInProgress
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Keeps the pages of the Discovery feed that the user is actually looking at up to date.
 *
 * Every [pollInterval] the visible pages older than [staleAfter] are re-requested and merged in place, so the feed
 * shows live probabilities and volumes without the user pulling anything. Only visible pages are refreshed: the
 * feed can hold many loaded pages, and the ones scrolled away change nothing on the screen.
 *
 * The ticker runs only while the screen is in the foreground — an app in the background and an open event sheet
 * both stop it — and the first tick after it comes back is immediate rather than a period late. Scrolling to a rest
 * ticks too: staleness is checked per page, so a tick over pages loaded seconds ago requests nothing.
 *
 * A failed refresh is silent by design: the page keeps its previous data, its [PageMeta.fetchedAt] does not move,
 * and the next tick tries again. Only after [reportAfterFailures] failures in a row on a page the user is looking at
 * does [onStaleData] fire, at most once per [reportCooldown] — by then the data is about a minute old.
 *
 * @param batchFlow pagination of the feed: the pages to refresh and the results of the refreshes
 * @param actionsFlow the flow the pagination takes its actions from
 * @param onStaleData reports that visible data has been failing to refresh for long enough to tell the user
 * @param timeSource measures page staleness and the toast cooldown
 */
@Suppress("LongParameterList")
internal class PolymarketFeedPoller(
    private val batchFlow: PolymarketEventsBatchFlow,
    private val actionsFlow: MutableSharedFlow<PolymarketEventsBatchAction>,
    private val onStaleData: () -> Unit,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val pollInterval: Duration = POLL_INTERVAL,
    private val staleAfter: Duration = STALE_AFTER,
    private val reportAfterFailures: Int = REPORT_AFTER_FAILURES,
    private val reportCooldown: Duration = REPORT_COOLDOWN,
) {

    private val inForeground = MutableStateFlow(value = false)
    private val visibleEventIds = MutableStateFlow(value = emptySet<String>())
    private val immediateTicks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val pageMeta = mutableMapOf<Int, PageMeta>()
    private val refreshesInFlight = mutableSetOf<Int>()

    private var config: PolymarketEventsListConfig? = null
    private var lastStaleDataReport: TimeMark? = null

    fun start(scope: CoroutineScope) {
        batchFlow.state
            .onEach(::stampLoadedPages)
            .launchIn(scope)

        batchFlow.updateResults
            .onEach { (request, result) -> applyRefreshResult(request = request, result = result) }
            .launchIn(scope)

        scope.launch {
            // Restarting the ticker on every foreground change gives the return its immediate tick for free.
            inForeground.collectLatest { isInForeground ->
                if (!isInForeground) return@collectLatest

                tick()
                while (true) {
                    delay(pollInterval)
                    tick()
                }
            }
        }

        scope.launch {
            immediateTicks.collect { tick() }
        }
    }

    /** The feed is loading its first page anew, so what was known about the previous pages no longer holds. */
    fun onFeedReloaded(config: PolymarketEventsListConfig) {
        this.config = config
        pageMeta.clear()
        refreshesInFlight.clear()
    }

    fun setVisibleEventIds(eventIds: Set<String>) {
        visibleEventIds.value = eventIds
    }

    fun setInForeground(isInForeground: Boolean) {
        inForeground.value = isInForeground
    }

    /** The user stopped scrolling: whatever is under their eyes now is worth a check ahead of the schedule. */
    fun onScrollIdle() {
        immediateTicks.tryEmit(Unit)
    }

    private fun stampLoadedPages(state: PolymarketEventsBatchListState) {
        val keys = state.data.mapTo(mutableSetOf()) { it.key }

        pageMeta.keys.retainAll(keys)
        refreshesInFlight.retainAll(keys)

        keys.forEach { key ->
            pageMeta.getOrPut(key) {
                val loadedAt = timeSource.markNow()
                PageMeta(fetchedAt = loadedAt, lastAttemptAt = loadedAt, failCount = 0)
            }
        }
    }

    private suspend fun tick() {
        if (!inForeground.value) return

        val config = config ?: return
        val state = batchFlow.state.value
        if (state.data.isEmpty()) return

        val stalePages = visibleBatchKeys(state).filter { key ->
            val meta = pageMeta[key] ?: return@filter false

            // A failed refresh leaves the page stale for good, so staleness alone would let every scroll-idle tick
            // ask again. The attempt clock is what paces a page that keeps failing.
            meta.fetchedAt.elapsedNow() >= staleAfter && meta.lastAttemptAt.elapsedNow() >= pollInterval
        }

        stalePages.forEach { key ->
            // A page whose refresh is still in flight is skipped rather than queued: the tick that follows the
            // slow one would otherwise stack a second request on the same page.
            if (!refreshesInFlight.add(key)) return@forEach

            pageMeta[key] = pageMeta.getValue(key).copy(lastAttemptAt = timeSource.markNow())

            TangemLogger.i("Feed: refreshing page $key")
            actionsFlow.emit(
                BatchAction.UpdateBatches(
                    keys = setOf(key),
                    updateRequest = PolymarketEventsUpdateRequest(batchKey = key, config = config),
                    async = true,
                    operationId = REFRESH_OPERATION_PREFIX + key,
                ),
            )
        }
    }

    private fun applyRefreshResult(
        request: PolymarketEventsUpdateRequest,
        result: BatchUpdateResult<Int, PolymarketEventsBatch>,
    ) {
        // Not an answer about the page: the pagination reports it when an operation is issued twice over.
        if (result is BatchUpdateResult.Error && result.throwable is OperationWIthTheSameIdInProgress) return

        val key = request.batchKey
        refreshesInFlight.remove(key)
        val meta = pageMeta[key] ?: return

        when (result) {
            is BatchUpdateResult.Success -> {
                pageMeta[key] = meta.copy(fetchedAt = timeSource.markNow(), failCount = 0)
            }
            is BatchUpdateResult.Error -> {
                val failCount = meta.failCount + 1
                TangemLogger.i("Feed: refresh of page $key failed, $failCount in a row")
                pageMeta[key] = meta.copy(failCount = failCount)
                reportStaleDataIfNeeded(key)
            }
        }
    }

    private fun reportStaleDataIfNeeded(key: Int) {
        val meta = pageMeta[key] ?: return
        if (meta.failCount < reportAfterFailures) return
        if (key !in visibleBatchKeys(batchFlow.state.value)) return

        val lastReport = lastStaleDataReport
        if (lastReport != null && lastReport.elapsedNow() < reportCooldown) return

        lastStaleDataReport = timeSource.markNow()
        onStaleData()
    }

    /**
     * Keys of the pages the visible cards come from.
     *
     * An event the cursor served twice is shown from the page it first appeared on — the same rule the feed
     * renders by — so a duplicate never marks a page the user cannot see as visible.
     */
    private fun visibleBatchKeys(state: PolymarketEventsBatchListState): Set<Int> {
        val visibleIds = visibleEventIds.value
        if (visibleIds.isEmpty()) return emptySet()

        val seenIds = mutableSetOf<String>()

        return buildSet {
            state.data.forEach { batch ->
                batch.data.events.forEach { event ->
                    if (seenIds.add(event.id) && event.id in visibleIds) add(batch.key)
                }
            }
        }
    }

    /**
     * @property fetchedAt when the page last arrived — by load or by refresh; a failed refresh leaves it alone
     * @property lastAttemptAt when the page was last asked for, successfully or not; paces the retries of a page
     *  that keeps failing, which staleness alone cannot do
     * @property failCount refreshes of this page that failed in a row
     */
    private data class PageMeta(val fetchedAt: TimeMark, val lastAttemptAt: TimeMark, val failCount: Int)

    private companion object {
        val POLL_INTERVAL = 20.seconds
        val STALE_AFTER = 20.seconds
        val REPORT_COOLDOWN = 5.minutes
        const val REPORT_AFTER_FAILURES = 3
        const val REFRESH_OPERATION_PREFIX = "polymarket-feed-refresh-"
    }
}