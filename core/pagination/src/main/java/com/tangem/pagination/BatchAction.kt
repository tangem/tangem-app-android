package com.tangem.pagination

/**
 * Action that can be dispatched to [BatchListSource].
 *
 * @param TRequest type of the request to load batches.
 * @param TKey type of the key of the batch.
 * @param TUpdate type of the update request.
 */
sealed class BatchAction<TRequest, TKey, TUpdate> {

    /**
     * Action to load the first batch.
     *
     * @param request request to load the first batch.
     */
    data class Reload<R>(
        val request: R,
    ) : BatchAction<R, Nothing, Nothing>()

    /**
     * Action to load the next batch.
     *
     * @param request request to load the next batch with new request.
     * If null, the last request will be used.
     * Will be saved in the state and used for future LoadMore actions with request = null.
     */
    data class LoadMore<R>(
        val request: R? = null,
    ) : BatchAction<R, Nothing, Nothing>()

    /**
     * Action to update the batch.
     *
     * @param keys keys of the batches to update.
     * @param request request to update the batches.
     */
    class UpdateBatches<K, U>(
        val keys: Set<K>,
        val request: U,
    ) : BatchAction<Nothing, K, U>()

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
    class CancelUpdates<K, U>(
        val predicate: (UpdateBatches<K, U>) -> Boolean,
    ) : BatchAction<Nothing, K, U>()
}
