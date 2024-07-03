package com.tangem.pagination

/**
 * Represents the result of a batch fetch operation.
 * Used in [BatchListState] and [BatchUpdateFetcher].
 *
 * @param TKey type of the key.
 * @param TData type of the data.
 * @param TError type of the error.
 */
sealed class FetchUpdateResult<out TKey, out TData, out TError> {

    /**
     * Represents a successful result of a batch update operation.
     *
     * @param data fetched data.
     */
    data class Success<TKey, TData>(
        val data: List<Batch<TKey, TData>>,
    ) : FetchUpdateResult<TKey, TData, Nothing>()

    /**
     * Represents an error result of a batch update operation.
     *
     * @param error error that occurred during the operation.
     */
    data class Error<TError>(val error: TError) : FetchUpdateResult<Nothing, Nothing, TError>()

    /**
     * Represents an unknown error result of a batch update operation.
     * Used for unexpected exceptions that occurred in `fetchUpdate` method in [BatchUpdateFetcher].
     *
     * @param throwable throwable that occurred during the operation.
     */
    class UnknownError(val throwable: Throwable) : FetchUpdateResult<Nothing, Nothing, Nothing>()
}
