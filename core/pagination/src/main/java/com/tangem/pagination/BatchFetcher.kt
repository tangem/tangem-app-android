package com.tangem.pagination

/**
 * Interface for fetching a batch of data. Used in [BatchListState].
 *
 * @param TRequest type of the request.
 * @param TData type of the data.
 * @param TError type of the error.
 *
 * @see BatchListState
 */
interface BatchFetcher<TRequest, TData, TError> {

    /**
     * Fetches a batch of data.
     *
     * @param request request to fetch the data.
     * @return result of the fetch operation.
     */
    suspend fun fetch(request: BatchRequest<TRequest>): BatchFetchResult<TData, TError>
}
