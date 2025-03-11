package com.tangem.pagination

/**
 * Represents the result of a batch fetch operation.
 * Used in [BatchListState] and [BatchUpdateFetcher].
 *
 * @param TKey type of the key.
 * @param TData type of the data.
 */
sealed class BatchUpdateResult<out TKey, out TData> {

    /**
     * Represents a successful result of a batch update operation.
     *
     * @param data fetched data.
     */
    data class Success<TKey, TData>(
        val data: List<Batch<TKey, TData>>,
    ) : BatchUpdateResult<TKey, TData>()

    /**
     * Represents an error result of a batch update operation.
     *
     * @param error error that occurred during the operation.
     */
    class Error(val throwable: Throwable) : BatchUpdateResult<Nothing, Nothing>()
}
