package com.tangem.pagination

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * Context for working with [BatchListSource].
 *
 * @param R type of the request.
 * @param K type of the key.
 * @param U type of the update request.
 *
 * @property actionsFlow flow of [BatchAction]s that would be dispatched to [BatchListSource].
 * @property coroutineScope scope for the [BatchListSource] to launch coroutines. When it is cancelled,
 * all the operations and requests launched in the [BatchListSource] would be cancelled and all data would be cleared.
 *
 * @see BatchListSource
 */
class BatchingContext<R, K, U>(
    val actionsFlow: Flow<BatchAction<R, K, U>>,
    val coroutineScope: CoroutineScope,
)