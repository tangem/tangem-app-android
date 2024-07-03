package com.tangem.pagination

/**
 * Represents a batch of data with a key.
 * Used in [BatchListState].
 *
 * @param K type of the key.
 * @param T type of the data.
 */
data class Batch<K, T>(
    val key: K,
    val data: T,
)
