package com.tangem.pagination

/**
 * Interface for fetching updates for a batch of data.
 * Used in [BatchListState].
 *
 * @param TKey type of the key.
 * @param TData type of the data.
 * @param TUpdate type of the update request.
 */
fun interface BatchUpdateFetcher<TKey, TData, TUpdate> {

    /**
     * Fetches updates for a batch of data.
     * Note that the result batch key as a result of executing the method must be presented in the [toUpdate] list,
     * otherwise, updates will not be performed
     *
     * @param toUpdate list of batches to update.
     * @param updateRequest request to update the data.
     * @return result of the update operation.
     */
    suspend fun fetchUpdate(toUpdate: List<Batch<TKey, TData>>, updateRequest: TUpdate): BatchUpdateResult<TKey, TData>
}