package com.tangem.pagination

/**
 * State that is used for listening the current state of a pagination.
 *
 * @param K type of the key of the batch.
 * @param T type of the data.
 * @param E type of the error.
 *
 * @property data list of loaded batches.
 * @property status current status of the pagination.
 *
 * @see BatchListSource
 */
data class BatchListState<K, T, E>(
    val data: List<Batch<K, T>>,
    val status: PaginationStatus<T, E>,
)