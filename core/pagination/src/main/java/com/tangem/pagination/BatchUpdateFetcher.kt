package com.tangem.pagination

/**
 * Interface for fetching updates for a batch of data.
 * Used in [BatchListState].
 *
 * @param TKey type of the key.
 * @param TData type of the data.
 * @param TUpdate type of the update request.
 */
interface BatchUpdateFetcher<TKey, TData, TUpdate> {

    /**
     * Fetches updates for a batch of data.
     * Note that the result batch key as a result of executing the method must be presented in the [toUpdate] list,
     * otherwise, updates will not be performed
     *
     * @param toUpdate list of batches to update.
     * @param updateRequest request to update the data.
     * @return result of the update operation.
     */
    suspend fun fetchUpdate(
        toUpdate: List<Batch<TKey, TData>>,
        updateRequest: TUpdate,
    ): BatchUpdateResult<TKey, TData> = BatchUpdateResult.Error(NotImplementedError())

    /**
     * Fetches updates for a batch of data asynchronously.
     * To update the data, use the [UpdateContext.update] method.
     * [UpdateContext.update] could be called as many times as you want.
     *
     * Note that the result batch key as a result of executing the method must be presented in the [toUpdate] list,
     * otherwise, updates will not be performed.
     *
     * @param toUpdate list of batches to update. **Attention** Data may be outdated and should be used only to make
     * a request for an update, not for the actual update operation. For the actual update operation, use the batches
     * provided by [UpdateContext.update].
     * @param updateRequest request to update the data.
     */
    suspend fun UpdateContext<TKey, TData>.fetchUpdateAsync(
        toUpdate: List<Batch<TKey, TData>>,
        updateRequest: TUpdate,
    ) {
    }

    /**
     * Context for updating the data.
     * Used by [BatchListSource] to provide a way to update batches by [fetchUpdateAsync] method.
     */
    interface UpdateContext<TKey, TData> {

        /**
         * Updates the data of the batch.
         * Could be called as many times as you want.
         *
         * Input batches keys and the data could not always be the same as the keys of the [toUpdate] list in
         * [fetchUpdateAsync] method, but the provided set of keys will always be a subset of the [toUpdate] list.
         *
         * @param update lambda to update the data.
         */
        suspend fun update(update: List<Batch<TKey, TData>>.() -> BatchUpdateResult<TKey, TData>)
    }
}