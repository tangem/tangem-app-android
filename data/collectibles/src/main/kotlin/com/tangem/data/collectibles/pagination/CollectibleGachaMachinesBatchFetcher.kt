package com.tangem.data.collectibles.pagination

import com.tangem.domain.collectibles.models.CollectibleGachaMachinesBatch
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesListConfig
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesPage
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import com.tangem.pagination.fetcher.BatchFetcher
import com.tangem.utils.coroutines.runSuspendCatching

/**
 * Pages the machines list with the cursor the backend returns in the response body.
 *
 * The stock [com.tangem.pagination.fetcher.CursorBatchFetcher] derives the next cursor from the last item of a
 * page, which this endpoint does not support: the cursor is a keyset token served alongside the machines.

 * @param fetchPage loads a single page: the filters to apply, the cursor of the previous page (`null` for the
 *  first one) and the page size.
 */
internal class CollectibleGachaMachinesBatchFetcher(
    private val batchSize: Int,
    private val fetchPage: suspend (
        config: CollectibleGachaMachinesListConfig,
        cursor: String?,
        limit: Int,
    ) -> CollectibleGachaMachinesPage,
) : BatchFetcher<CollectibleGachaMachinesListConfig, CollectibleGachaMachinesBatch> {

    private var lastConfig: CollectibleGachaMachinesListConfig? = null
    private var nextCursor: String? = null

    override suspend fun fetchFirst(
        requestParams: CollectibleGachaMachinesListConfig,
    ): BatchFetchResult<CollectibleGachaMachinesBatch> {
        lastConfig = requestParams
        nextCursor = null
        return runFetch(config = requestParams, cursor = null)
    }

    override suspend fun fetchNext(
        overrideRequestParams: CollectibleGachaMachinesListConfig?,
        lastResult: BatchFetchResult<CollectibleGachaMachinesBatch>,
    ): BatchFetchResult<CollectibleGachaMachinesBatch> {
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
        return runFetch(config = config, cursor = nextCursor)
    }

    private suspend fun runFetch(
        config: CollectibleGachaMachinesListConfig,
        cursor: String?,
    ): BatchFetchResult<CollectibleGachaMachinesBatch> = runSuspendCatching { fetchPage(config, cursor, batchSize) }
        .fold(
            onSuccess = { page -> toBatchResult(page = page, requestCursor = cursor) },
            // A failed request is the pagination's to report, so the batch turns into a retryable error.
            onFailure = { throwable -> BatchFetchResult.Error(throwable) },
        )

    private fun toBatchResult(
        page: CollectibleGachaMachinesPage,
        requestCursor: String?,
    ): BatchFetchResult<CollectibleGachaMachinesBatch> {
        nextCursor = page.cursor

        return BatchFetchResult.Success(
            data = CollectibleGachaMachinesBatch(
                collectibleGachaMachines = page.collectibleGachaMachines,
                requestCursor = requestCursor,
            ),
            empty = page.collectibleGachaMachines.isEmpty(),
            last = !page.hasNext || page.cursor == null,
        )
    }
}