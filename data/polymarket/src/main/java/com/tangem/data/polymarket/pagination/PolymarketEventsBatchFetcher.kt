package com.tangem.data.polymarket.pagination

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
 * Every page keeps the cursor it was requested with, which is what [PolymarketEventsUpdateFetcher] refreshes by.
 *
 * Failures are retried once, silently, after [retryDelay] — the caller keeps its loader up meanwhile, so a single
 * hiccup never surfaces as an error state.
 *
 * An empty page is not a failure, and a page shorter than [batchSize] is not the end: the BFF decides both with
 * `hasNext` and the cursor. An empty page that has more behind it is followed at once, through up to
 * [MAX_EMPTY_PAGES_FOLLOWED] empty pages, so what the caller receives either carries events or ends the feed —
 * the list never has to ask for a page nothing on screen would trigger.
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
        return fetchFollowingEmptyPages(config = requestParams, cursor = null)
    }

    override suspend fun fetchNext(
        overrideRequestParams: PolymarketEventsListConfig?,
        lastResult: BatchFetchResult<PolymarketEventsBatch>,
    ): BatchFetchResult<PolymarketEventsBatch> {
        val config = overrideRequestParams
            ?: lastConfig
            ?: error("fetchFirst() must be called before fetchNext()")

        if (lastResult is BatchFetchResult.Success && lastResult.isLast && overrideRequestParams == null) {
            return BatchFetchResult.Error(EndOfPaginationException())
        }

        // A successful page without a cursor has nothing to continue from; a failed one is retried with the cursor
        // it was requested with, which is still the one held here.
        if (lastResult is BatchFetchResult.Success && nextCursor == null) {
            return BatchFetchResult.Error(EndOfPaginationException())
        }

        lastConfig = config
        return fetchFollowingEmptyPages(config = config, cursor = nextCursor)
    }

    private suspend fun fetchFollowingEmptyPages(
        config: PolymarketEventsListConfig,
        cursor: String?,
    ): BatchFetchResult<PolymarketEventsBatch> {
        var requestCursor = cursor
        var emptyPagesFollowed = 0

        while (true) {
            val page = fetchWithSilentRetry(config = config, cursor = requestCursor)
                // A failed request is the pagination's to report, so the batch turns into a retryable error.
                .getOrElse { throwable -> return BatchFetchResult.Error(throwable) }

            val responseCursor = page.cursor?.takeIf(String::isNotBlank)
            val hasMore = page.hasNext && responseCursor != null
            val isFollowed = page.events.isEmpty() && hasMore && emptyPagesFollowed < MAX_EMPTY_PAGES_FOLLOWED

            if (!isFollowed) {
                nextCursor = responseCursor
                return BatchFetchResult.Success(
                    data = PolymarketEventsBatch(events = page.events, requestCursor = requestCursor),
                    isEmpty = page.events.isEmpty(),
                    isLast = !hasMore || page.events.isEmpty(),
                )
            }

            emptyPagesFollowed++
            requestCursor = responseCursor
        }
    }

    private suspend fun fetchWithSilentRetry(
        config: PolymarketEventsListConfig,
        cursor: String?,
    ): Result<PolymarketEventsPage> {
        val firstAttempt = runSuspendCatching { fetchPage(config, cursor, batchSize) }
        if (firstAttempt.isSuccess) return firstAttempt

        delay(retryDelay)

        return runSuspendCatching { fetchPage(config, cursor, batchSize) }
    }

    private companion object {
        val RETRY_DELAY = 2.seconds

        /** A guard against a feed that keeps promising more and serving nothing; past it the feed is ended. */
        const val MAX_EMPTY_PAGES_FOLLOWED = 3
    }
}