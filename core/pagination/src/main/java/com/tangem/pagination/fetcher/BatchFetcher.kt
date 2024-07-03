package com.tangem.pagination.fetcher

import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState

/**
 * Interface for fetching a batch of data. Used in [BatchListState].
 *
 * @param TRequest type of the request.
 * @param TData type of the data.
 * @param TError type of the error.
 *
 * @see BatchListState
 */
interface BatchFetcher<TRequest : Any, TData, TError> {

    /**
     * Fetches the first batch of data.
     *
     * @param request initial request. Will be saved to be used in [fetchNext] requests.
     * @return result of the fetch operation.
     */
    suspend fun fetchFirst(request: TRequest): BatchFetchResult<TData, TError>

    /**
     * Fetches the next batch of data.
     *
     * @param overrideRequest overrides current remembered request, even if that fetch fails.
     * If null, the last request should be used.
     * @param lastResult result of the last fetch operation.
     * @return result of the fetch operation.
     */
    suspend fun fetchNext(
        overrideRequest: TRequest?,
        lastResult: BatchFetchResult<TData, TError>,
    ): BatchFetchResult<TData, TError>
}