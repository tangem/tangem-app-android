package com.tangem.pagination

/**
 * Represents a result of a batch fetch request.
 * Used in [BatchListState].
 *
 * @param TData type of the data.
 */
sealed class BatchFetchResult<out TData> {

    /**
     * Represents a successful result of a batch fetch request.
     *
     * @param data fetched data.
     * @param empty indicates that data is empty and [BatchListSource] shouldn't create new batch for this result
     * @param last indicates if this is the last batch for the request.
     */
    data class Success<TData>(
        val data: TData,
        val empty: Boolean,
        val last: Boolean,
    ) : BatchFetchResult<TData>()

    /**
     * Represents an error result of a batch fetch request.
     * Also used for unexpected exceptions that occurred in fetch method in BatchFetcher.
     *
     * @param throwable throwable that occurred during the request.
     * @see com.tangem.pagination.fetcher.BatchFetcher
     */
    class Error(val throwable: Throwable) : BatchFetchResult<Nothing>()
}