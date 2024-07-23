package com.tangem.pagination

import java.util.UUID

/**
 * Action that can be dispatched to [BatchListSource].
 *
 * @param TRequestParams type of the request params to load batches.
 * @param TKey type of the key of the batch.
 * @param TUpdate type of the update request.
 */
sealed class BatchAction<out TKey, out TRequestParams, out TUpdate> {

    /**
     * Action to load the first batch.
     *
     * @param requestParams request params to load the first batch.
     */
    data class Reload<TRequestParams : Any>(
        val requestParams: TRequestParams,
    ) : BatchAction<Nothing, TRequestParams, Nothing>()

    /**
     * Action to load the next batch.
     *
     * @param requestParams request params to load the next batch with new request.
     * If null, the last request will be used.
     * Will be saved in the state and used for future LoadMore actions with request = null.
     */
    data class LoadMore<TRequestParams : Any>(
        val requestParams: TRequestParams? = null,
    ) : BatchAction<Nothing, TRequestParams, Nothing>()

    /**
     * Action to update the batch.
     *
     * @param keys keys of the batches to update.
     * @param updateRequest request to update the batches.
     * @param async true if the request doesn't require to synchronize on specific batches in order to fetch update
     * data, this request will be delegated to fetchAsync method in [BatchUpdateFetcher],
     * false if request requires to hold the current batches data until fetch + update is completed
     * @param operationId the unique identifier of the request.
     * Only one request with the same hash can be executed at a time,
     * the rest of the requests will be canceled as long as there is a request with this hash in progress.
     */
    class UpdateBatches<TKey, TUpdate>(
        val keys: Set<TKey>,
        val updateRequest: TUpdate,
        val async: Boolean = false,
        val operationId: String = UUID.randomUUID().toString(),
    ) : BatchAction<TKey, Nothing, TUpdate>()

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
    ) : BatchAction<TKey, Nothing, TUpdate>()

    /**
     * Clears the state and stops all current batch loading and updates
     * After this status becomes [PaginationStatus.None]
     */
    data object Reset : BatchAction<Nothing, Nothing, Nothing>()
}