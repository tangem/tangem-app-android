package com.tangem.pagination

/**
 * Action that can be dispatched to [BatchListSource].
 *
 * @param TRequestParams type of the request params to load batches.
 * @param TKey type of the key of the batch.
 * @param TUpdate type of the update request.
 */
sealed class BatchAction<TRequestParams, TKey, TUpdate> {

    /**
     * Action to load the first batch.
     *
     * @param requestParams request params to load the first batch.
     */
    data class Reload<TRequestParams : Any>(
        val requestParams: TRequestParams,
    ) : BatchAction<TRequestParams, Nothing, Nothing>()

    /**
     * Action to load the next batch.
     *
     * @param requestParams request params to load the next batch with new request.
     * If null, the last request will be used.
     * Will be saved in the state and used for future LoadMore actions with request = null.
     */
    data class LoadMore<TRequestParams : Any>(
        val requestParams: TRequestParams? = null,
    ) : BatchAction<TRequestParams, Nothing, Nothing>()

    /**
     * Action to update the batch.
     *
     * @param keys keys of the batches to update.
     * @param updateRequest request to update the batches.
     */
    class UpdateBatches<TKey, TUpdate>(
        val keys: Set<TKey>,
        val updateRequest: TUpdate,
    ) : BatchAction<Nothing, TKey, TUpdate>()

    /**
     * Action to cancel the current batch loading.
     */
    data object CancelBatchLoading : BatchAction<Nothing, Nothing, Nothing>()

    /**
     * Action to cancel all update requests.
     */
    data object CancelAllUpdates : BatchAction<Nothing, Nothing, Nothing>()

    /**
     * Action to cancel update requests that satisfy the predicate.
     *
     * @param predicate predicate to check if the update request should be cancelled.
     */
    class CancelUpdates<TKey, TUpdate>(
        val predicate: (UpdateBatches<TKey, TUpdate>) -> Boolean,
    ) : BatchAction<Nothing, TKey, TUpdate>()
}