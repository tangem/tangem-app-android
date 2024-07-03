package com.tangem.pagination

/**
 * Status of the pagination.
 *
 * @param T type of the data.
 * @param E type of the error.
 *
 * @see BatchListState
 */
sealed class PaginationStatus<out T, out E> {

    /**
     * Represents that there is no data. Used when the list of batches is empty.
     * The initial state of the pagination.
     */
    data object None : PaginationStatus<Nothing, Nothing>()

    /**
     * Represents that the batch is loading for the first time.
     * Used when the pagination is empty and the first batch is being loaded.
     */
    data object InitialLoading : PaginationStatus<Nothing, Nothing>()

    /**
     * Represents that the first batch was loaded with an error.
     *
     * @param error error that occurred during the initial loading.
     */
    data class InitialLoadingError<T, E>(
        val error: E?,
    ) : PaginationStatus<T, E>()

    /**
     * Represents that the last batch was loaded and
     * the source is ready to load the next one or reload previous if [lastResult] is an error.
     * For the first batch, [lastResult] is always [FetchResult.Success]
     *
     * @param lastResult result of the last batch fetch.
     */
    data class Paginating<out T, out E>(
        val lastResult: FetchResult<T, E>,
    ) : PaginationStatus<T, E>()

    /**
     * Represents that the next batch is loading.
     * Used when the next batch is being loaded.
     */
    data object NextBatchLoading : PaginationStatus<Nothing, Nothing>()

    /**
     * Represents that the source has no more batches to load.
     * The next [BatchAction.LoadMore] with [BatchAction.LoadMore.request] = null will be ignored.
     */
    data object EndOfPagination : PaginationStatus<Nothing, Nothing>()
}
