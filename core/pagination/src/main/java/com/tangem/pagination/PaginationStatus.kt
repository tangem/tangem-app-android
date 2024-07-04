package com.tangem.pagination

/**
 * Status of the pagination.
 *
 * @param TData type of the data.
 *
 * @see BatchListState
 */
sealed class PaginationStatus<out TData> {

    /**
     * Represents that there is no data. Used when the list of batches is empty.
     * The initial state of the pagination.
     */
    data object None : PaginationStatus<Nothing>()

    /**
     * Represents that the batch is loading for the first time.
     * Used when the pagination is empty and the first batch is being loaded.
     */
    data object InitialLoading : PaginationStatus<Nothing>()

    /**
     * Represents that the first batch was loaded with an error.
     *
     * @param error error that occurred during the initial loading.
     */
    data class InitialLoadingError(
        val throwable: Throwable,
    ) : PaginationStatus<Nothing>()

    /**
     * Represents that the last batch was loaded and
     * the source is ready to load the next one or reload previous if [lastResult] is an error.
     * For the first batch, [lastResult] is always [BatchFetchResult.Success]
     *
     * @param lastResult result of the last batch fetch.
     */
    data class Paginating<out TData>(
        val lastResult: BatchFetchResult<TData>,
    ) : PaginationStatus<TData>()

    /**
     * Represents that the next batch is loading.
     * Used when the next batch is being loaded.
     */
    data object NextBatchLoading : PaginationStatus<Nothing>()

    /**
     * Represents that the source has no more batches to load.
     * The next [BatchAction.LoadMore] with [BatchAction.LoadMore.requestParams] = null will be ignored.
     */
    data object EndOfPagination : PaginationStatus<Nothing>()
}