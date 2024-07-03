package com.tangem.pagination

/**
 * Request to fetch a batch of data.
 *
 * @param R type of the request.
 *
 * @property offset offset for the request.
 * @property limit limit for the request.
 * @property data body of the request.
 *
 * @see BatchFetcher
 */
data class BatchRequest<R>(
    val offset: Int,
    val limit: Int,
    val data: R,
)