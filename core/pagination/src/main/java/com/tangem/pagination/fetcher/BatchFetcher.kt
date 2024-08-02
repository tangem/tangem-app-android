package com.tangem.pagination.fetcher

import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState

/**
 * Interface for fetching a batch of data. Used in [BatchListState].
 *
 * @param TRequestParams type of the request.
 * @param TData type of the data.
 *
 * @see BatchListState
 */
interface BatchFetcher<TRequestParams : Any, TData> {

    /**
     * Fetches the first batch of data.
     *
     * @param requestParams initial request params. Will be saved to be used in [fetchNext] requests.
     * @return result of the fetch operation.
     */
    suspend fun fetchFirst(requestParams: TRequestParams): BatchFetchResult<TData>

    /**
     * Fetches the next batch of data.
     *
     * @param overrideRequestParams overrides current remembered request, even if that fetch fails.
     * If null, the last request should be used.
     * @param lastResult result of the last fetch operation.
     * @return result of the fetch operation.
     */
    suspend fun fetchNext(
        overrideRequestParams: TRequestParams?,
        lastResult: BatchFetchResult<TData>,
    ): BatchFetchResult<TData>
}
