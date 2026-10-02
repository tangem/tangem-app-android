package com.tangem.data.polymarket.pagination

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.domain.polymarket.model.PolymarketActivityError
import com.tangem.domain.polymarket.model.PolymarketActivityListConfig
import com.tangem.domain.polymarket.model.PolymarketActivityPage
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import com.tangem.pagination.fetcher.BatchFetcher
import com.tangem.utils.coroutines.runSuspendCatching

internal class PolymarketActivityBatchFetcher(
    private val batchSize: Int,
    private val fetchPage: suspend (PolymarketActivityListConfig, String?, Int) ->
    Either<PolymarketActivityError, PolymarketActivityPage>,
) : BatchFetcher<PolymarketActivityListConfig, List<PolymarketActivity>> {

    private var config: PolymarketActivityListConfig? = null
    private var nextCursor: String? = null
    private val cursors = mutableSetOf<String>()

    override suspend fun fetchFirst(
        requestParams: PolymarketActivityListConfig,
    ): BatchFetchResult<List<PolymarketActivity>> {
        config = requestParams
        nextCursor = null
        cursors.clear()
        return fetch(requestParams)
    }

    override suspend fun fetchNext(
        overrideRequestParams: PolymarketActivityListConfig?,
        lastResult: BatchFetchResult<List<PolymarketActivity>>,
    ): BatchFetchResult<List<PolymarketActivity>> {
        if (overrideRequestParams != null) return fetchFirst(overrideRequestParams)
        if (nextCursor == null) return BatchFetchResult.Error(EndOfPaginationException())
        return fetch(requireNotNull(config))
    }

    private suspend fun fetch(config: PolymarketActivityListConfig): BatchFetchResult<List<PolymarketActivity>> =
        runSuspendCatching {
            val page = fetchPage(config, nextCursor, batchSize).getOrElse { error("Activity request failed: $it") }
            require(page.nextCursor == null || page.nextCursor !in cursors) { "Activity cursor did not advance" }
            page.nextCursor?.let(cursors::add)
            nextCursor = page.nextCursor
            BatchFetchResult.Success(data = page.items, empty = page.items.isEmpty(), last = nextCursor == null)
        }.getOrElse { BatchFetchResult.Error(it) }
}