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
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Keeps the visible pages of the Discovery feed up to date. */
@Suppress("LongParameterList")
internal class PolymarketFeedPoller(
    private val scope: CoroutineScope,
    private val batchFlow: PolymarketEventsBatchFlow,
    private val actionsFlow: MutableSharedFlow<PolymarketEventsBatchAction>,
    private val onStaleData: () -> Unit,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val pollInterval: Duration = POLL_INTERVAL,
    private val staleAfter: Duration = STALE_AFTER,
    private val reportAfterFailures: Int = REPORT_AFTER_FAILURES,
) {

    private val tickerJob = JobHolder()

    private val pageMeta = mutableMapOf<Int, PageMeta>()
    private val refreshesInFlight = mutableSetOf<Int>()

    private var visibleEventIds: Set<String> = emptySet()
    private var config: PolymarketEventsListConfig? = null

    /** Relies on [scope] being single-threaded: [pageMeta] and [refreshesInFlight] are unguarded. */
    fun start() {
        batchFlow.state
            .onEach(::stampLoadedPages)
            .launchIn(scope)

        batchFlow.updateResults
            .onEach { (request, result) -> applyRefreshResult(request = request, result = result) }
            .launchIn(scope)
    }

    fun resume() {
        scope.launch {
            tick()

            while (true) {
                delay(pollInterval)
                tick()
            }
        }.saveIn(tickerJob)
    }

    fun pause() {
        tickerJob.cancel()
    }

    fun onFeedReloaded(config: PolymarketEventsListConfig) {
        this.config = config
        pageMeta.clear()
        refreshesInFlight.clear()
    }

    fun setVisibleEventIds(eventIds: Set<String>) {
        this.visibleEventIds = eventIds
    }

    fun onScrollIdle() {
        if (!tickerJob.isActive) return

        scope.launch { tick() }
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
        val config = config ?: return
        val state = batchFlow.state.value
        if (state.data.isEmpty()) return

        val stalePages = visibleBatchKeys(state).filter { key ->
            val meta = pageMeta[key] ?: return@filter false

            meta.fetchedAt.elapsedNow() >= staleAfter && meta.lastAttemptAt.elapsedNow() >= pollInterval
        }

        stalePages.forEach { key ->
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
        if (result is BatchUpdateResult.Error && result.throwable is OperationWIthTheSameIdInProgress) return

        val key = request.batchKey
        refreshesInFlight.remove(key)
        val meta = pageMeta[key] ?: return

        when (result) {
            is BatchUpdateResult.Success -> {
                pageMeta[key] = meta.copy(fetchedAt = timeSource.markNow(), failCount = 0)
            }
            is BatchUpdateResult.Error -> {
                if (result.throwable is CancellationException) return

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

        onStaleData()
    }

    /** An event served twice counts towards the page it first appeared on — the rule the feed renders by. */
    private fun visibleBatchKeys(state: PolymarketEventsBatchListState): Set<Int> {
        val visibleIds = visibleEventIds
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

    /** [fetchedAt] moves on success only, [lastAttemptAt] on every request. */
    private data class PageMeta(val fetchedAt: TimeMark, val lastAttemptAt: TimeMark, val failCount: Int)

    private companion object {
        val POLL_INTERVAL = 20.seconds
        val STALE_AFTER = 20.seconds
        const val REPORT_AFTER_FAILURES = 3
        const val REFRESH_OPERATION_PREFIX = "polymarket-feed-refresh-"
    }
}