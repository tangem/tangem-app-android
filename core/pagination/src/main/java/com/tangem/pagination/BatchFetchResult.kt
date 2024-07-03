package com.tangem.pagination

/**
 * Represents a result of a batch fetch request.
 * Used in [BatchListState].
 *
 * @param T type of the data.
 * @param E type of the error.
 */
sealed class BatchFetchResult<out T, out E> {

    /**
     * Represents a successful result of a batch fetch request.
     *
     * @param data fetched data.
     * @param last indicates if this is the last batch for the request.
     */
    data class Success<T>(
        val data: T,
        val last: Boolean = false,
    ) : BatchFetchResult<T, Nothing>()

    /**
     * Represents an error result of a batch fetch request.
     *
     * @param error error that occurred during the request.
     */
    data class Error<E>(val error: E) : BatchFetchResult<Nothing, E>()

    /**
     * Represents an unknown error result of a batch fetch request.
     * Used for unexpected exceptions that occurred in fetch method in [BatchFetcher].
     *
     * @param throwable throwable that occurred during the request.
     */
    class UnknownError(val throwable: Throwable) : BatchFetchResult<Nothing, Nothing>()
}
