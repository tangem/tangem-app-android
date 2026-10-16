package com.tangem.data.polymarket.pagination

import arrow.core.Either
import com.tangem.domain.polymarket.error.PolymarketPositionsReadException
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsPage
import com.tangem.domain.polymarket.model.PolymarketPositionsPageRequest
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import com.tangem.pagination.fetcher.BatchFetcher

/**
 * Pages a positions list with the cursor the BFF returns alongside each page.
 *
 * The stock [com.tangem.pagination.fetcher.CursorBatchFetcher] derives the next cursor from the last item of a
 * page, which this endpoint does not support. A failed page is retried with the request it failed on, and a
 * cursor the list has already served is refused as a loop rather than followed.
 */
internal class PolymarketPositionsBatchFetcher(
    private val batchSize: Int,
    private val fetchPage: suspend (config: PolymarketPositionsListConfig, request: PolymarketPositionsPageRequest) ->
    Either<PolymarketPositionsError, PolymarketPositionsPage<PolymarketPosition>>,
) : BatchFetcher<PolymarketPositionsListConfig, List<PolymarketPosition>> {

    private var config: PolymarketPositionsListConfig? = null
    private var next: PolymarketPositionsPageRequest? = null
    private val cursors = mutableSetOf<String>()

    init {
        require(batchSize in 1..MAX_PAGE_SIZE)
    }

    override suspend fun fetchFirst(
        requestParams: PolymarketPositionsListConfig,
    ): BatchFetchResult<List<PolymarketPosition>> {
        config = requestParams
        next = PolymarketPositionsPageRequest.First(batchSize)
        cursors.clear()
        return fetch(requestParams)
    }

    override suspend fun fetchNext(
        overrideRequestParams: PolymarketPositionsListConfig?,
        lastResult: BatchFetchResult<List<PolymarketPosition>>,
    ): BatchFetchResult<List<PolymarketPosition>> {
        if (overrideRequestParams != null) return fetchFirst(overrideRequestParams)
        return fetch(requireNotNull(config) { "fetchFirst() must be called before fetchNext()" })
    }

    private suspend fun fetch(config: PolymarketPositionsListConfig): BatchFetchResult<List<PolymarketPosition>> {
        val request = next ?: return BatchFetchResult.Error(EndOfPaginationException())
        return fetchPage(config, request).fold(
            ifLeft = { BatchFetchResult.Error(PolymarketPositionsReadException(it)) },
            ifRight = { page ->
                val cursor = page.nextCursor
                if (cursor != null && !cursors.add(cursor)) {
                    return BatchFetchResult.Error(IllegalStateException("Positions cursor did not advance"))
                }
                next = cursor?.let(PolymarketPositionsPageRequest::Next)
                BatchFetchResult.Success(data = page.positions, empty = page.positions.isEmpty(), last = cursor == null)
            },
        )
    }

    private companion object {
        const val MAX_PAGE_SIZE = 30
    }
}