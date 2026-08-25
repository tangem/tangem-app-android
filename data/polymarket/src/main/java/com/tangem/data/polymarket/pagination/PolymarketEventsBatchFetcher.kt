package com.tangem.data.polymarket.pagination

import com.tangem.domain.polymarket.error.PolymarketEmptyFeedException
import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketEventsPage
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import com.tangem.pagination.fetcher.BatchFetcher
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Pages the Discovery feed with the cursor the BFF returns in the response body.
 *
 * The stock [com.tangem.pagination.fetcher.CursorBatchFetcher] derives the next cursor from the last item of a
 * page, which this endpoint does not support: the cursor is a keyset token served alongside the events.
 *
 * Every page keeps the cursor it was requested with, so it can be asked for again — that is what
 * [PolymarketEventsUpdateFetcher] refreshes it by.
 *
 * Failures are retried once, silently, after [retryDelay] — the caller keeps its loader up meanwhile, so a single
 * hiccup never surfaces as an error state. An empty **first** page is retried the same way and, if it stays empty,
 * reported as [PolymarketEmptyFeedException]: an empty category looks exactly like a failed load to the user.
 * An empty **next** page is just the end of the feed.
 *
 * @param fetchPage loads a single page: the category to filter by, the cursor of the previous page (`null` for the
 *  first one) and the page size.
 */
internal class PolymarketEventsBatchFetcher(
    private val batchSize: Int,
    private val retryDelay: Duration = RETRY_DELAY,
    private val fetchPage: suspend (config: PolymarketEventsListConfig, cursor: String?, limit: Int) ->
    PolymarketEventsPage,
) : BatchFetcher<PolymarketEventsListConfig, PolymarketEventsBatch> {

    private var lastConfig: PolymarketEventsListConfig? = null
    private var nextCursor: String? = null

    override suspend fun fetchFirst(
        requestParams: PolymarketEventsListConfig,
    ): BatchFetchResult<PolymarketEventsBatch> {
        lastConfig = requestParams
        nextCursor = null
        return fetchWithSilentRetry(config = requestParams, cursor = null, isFirstBatch = true)
    }

    override suspend fun fetchNext(
        overrideRequestParams: PolymarketEventsListConfig?,
        lastResult: BatchFetchResult<PolymarketEventsBatch>,
    ): BatchFetchResult<PolymarketEventsBatch> {
        val config = overrideRequestParams
            ?: lastConfig
            ?: error("fetchFirst() must be called before fetchNext()")

        if (lastResult is BatchFetchResult.Success && lastResult.last && overrideRequestParams == null) {
            return BatchFetchResult.Error(EndOfPaginationException())
        }

        // A successful page without a cursor has nothing to continue from; a failed one is retried with the cursor
        // it was requested with, which is still the one held here.
        if (lastResult is BatchFetchResult.Success && nextCursor == null) {
            return BatchFetchResult.Error(EndOfPaginationException())
        }

        lastConfig = config
        return fetchWithSilentRetry(config = config, cursor = nextCursor, isFirstBatch = false)
    }

    private suspend fun fetchWithSilentRetry(
        config: PolymarketEventsListConfig,
        cursor: String?,
        isFirstBatch: Boolean,
    ): BatchFetchResult<PolymarketEventsBatch> {
        val firstAttempt = runFetch(config = config, cursor = cursor, isFirstBatch = isFirstBatch)
        if (firstAttempt is BatchFetchResult.Success) return firstAttempt

        delay(retryDelay)

        return runFetch(config = config, cursor = cursor, isFirstBatch = isFirstBatch)
    }

    private suspend fun runFetch(
        config: PolymarketEventsListConfig,
        cursor: String?,
        isFirstBatch: Boolean,
    ): BatchFetchResult<PolymarketEventsBatch> = runSuspendCatching { fetchPage(config, cursor, batchSize) }
        .fold(
            onSuccess = { page -> toBatchResult(page = page, isFirstBatch = isFirstBatch, requestCursor = cursor) },
            // A failed request is the pagination's to report, so the batch turns into a retryable error.
            onFailure = { throwable -> BatchFetchResult.Error(throwable) },
        )

    private fun toBatchResult(
        page: PolymarketEventsPage,
        isFirstBatch: Boolean,
        requestCursor: String?,
    ): BatchFetchResult<PolymarketEventsBatch> {
        if (isFirstBatch && page.events.isEmpty()) {
            return BatchFetchResult.Error(PolymarketEmptyFeedException())
        }

        nextCursor = page.cursor

        return BatchFetchResult.Success(
            data = PolymarketEventsBatch(events = page.events, requestCursor = requestCursor),
            empty = page.events.isEmpty(),
            last = !page.hasNext || page.cursor == null,
        )
    }

    private companion object {
        val RETRY_DELAY = 2.seconds
    }
}