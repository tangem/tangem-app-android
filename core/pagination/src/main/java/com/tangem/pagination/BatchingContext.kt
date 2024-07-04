package com.tangem.pagination

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * Context for working with [BatchListSource].
 *
 * @param TRequestParams type of the request.
 * @param TKey type of the key.
 * @param TUpdate type of the update request.
 *
 * @property actionsFlow flow of [BatchAction]s that would be dispatched to [BatchListSource].
 * @property coroutineScope scope for the [BatchListSource] to launch coroutines. When it is cancelled,
 * all the operations and requests launched in the [BatchListSource] would be cancelled and all data would be cleared.
 *
 * @see BatchListSource
 */
class BatchingContext<TKey, TRequestParams, TUpdate>(
    val actionsFlow: Flow<BatchAction<TKey, TRequestParams, TUpdate>>,
    val coroutineScope: CoroutineScope,
)