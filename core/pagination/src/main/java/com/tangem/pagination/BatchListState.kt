package com.tangem.pagination

/**
 * State that is used for listening the current state of a pagination.
 *
 * @param TKey type of the key of the batch.
 * @param TData type of the data.
 * @param TError type of the error.
 *
 * @property data list of loaded batches.
 * @property status current status of the pagination.
 *
 * @see BatchListSource
 */
data class BatchListState<TKey, TData, TError>(
    val data: List<Batch<TKey, TData>>,
    val status: PaginationStatus<TData, TError>,
)
