package com.tangem.pagination

/**
 * Interface for fetching updates for a batch of data.
 * Used in [BatchListState].
 *
 * @param TKey type of the key.
 * @param TData type of the data.
 * @param TError type of the error.
 * @param TUpdate type of the update request.
 */
interface BatchUpdateFetcher<TKey, TData, TError, TUpdate> {

    /**
     * Fetches updates for a batch of data.
     *
     * @param toUpdate list of batches to update.
     * @param updateRequest request to update the data.
     * @return result of the update operation.
     */
    suspend fun fetchUpdate(
        toUpdate: List<Batch<TKey, TData>>,
        updateRequest: TUpdate,
    ): BatchFetchUpdateResult<TKey, TData, TError>
}
