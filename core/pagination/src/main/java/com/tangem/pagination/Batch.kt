package com.tangem.pagination

/**
 * Represents a batch of data with a key.
 * Used in [BatchListState].
 *
 * @param TKey type of the key.
 * @param TData type of the data.
 */
data class Batch<TKey, TData>(
    val key: TKey,
    val data: TData,
)