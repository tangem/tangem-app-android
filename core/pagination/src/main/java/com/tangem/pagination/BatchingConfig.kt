package com.tangem.pagination

/**
 * Configuration for batching.
 *
 * @param batchSize size of the batch.
 * @param prefetchDistance number of items to fetch for the first batch.
 */
data class BatchingConfig(
    val batchSize: Int,
    val prefetchDistance: Int = batchSize,
)
