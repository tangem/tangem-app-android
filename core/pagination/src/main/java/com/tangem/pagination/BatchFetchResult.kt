package com.tangem.pagination

/**
 * Represents a result of a batch fetch request.
 * Used in [BatchListState].
 *
 * @param TData type of the data.
 * @param TError type of the error.
 */
sealed class BatchFetchResult<out TData, out TError> {

    /**
     * Represents a successful result of a batch fetch request.
     *
     * @param data fetched data.
     * @param last indicates if this is the last batch for the request.
     */
    data class Success<TData>(
        val data: TData,
        val last: Boolean = false,
    ) : BatchFetchResult<TData, Nothing>()

    /**
     * Represents an error result of a batch fetch request.
     *
     * @param error error that occurred during the request.
     */
    data class Error<TError>(val error: TError) : BatchFetchResult<Nothing, TError>()

    /**
     * Represents an unknown error result of a batch fetch request.
     * Used for unexpected exceptions that occurred in fetch method in BatchFetcher.
     *
     * @param throwable throwable that occurred during the request.
     * @see com.tangem.pagination.fetcher.BatchFetcher
     */
    class UnknownError(val throwable: Throwable) : BatchFetchResult<Nothing, Nothing>()
}