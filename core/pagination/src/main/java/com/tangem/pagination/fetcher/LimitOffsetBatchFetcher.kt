package com.tangem.pagination.fetcher

import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fetcher that uses limit and offset to fetch data.
 *
 * @param TRequest type of the request.
 * @param TData type of the data.
 * @param TError type of the error.
 *
 * @property prefetchDistance number of items to fetch for the first batch.
 * @property batchSize size of the batch.
 * @property fetch function that fetches the data.
 */
class LimitOffsetBatchFetcher<TRequest : Any, TData, TError>(
    private val prefetchDistance: Int,
    private val batchSize: Int,
    private val fetch: (request: Request<TRequest>) -> BatchFetchResult<TData, TError>,
) : BatchFetcher<TRequest, TData, TError> {

    data class Request<TRequest>(
        val limit: Int,
        val offset: Int,
        val request: TRequest,
    )

    private val lastRequest = MutableStateFlow<Request<TRequest>?>(null)

    override suspend fun fetchFirst(request: TRequest): BatchFetchResult<TData, TError> {
        val req = Request(
            offset = 0,
            limit = prefetchDistance,
            request = request,
        )

        val res = fetch(req)
        lastRequest.value = req
        return res
    }

    override suspend fun fetchNext(
        overrideRequest: TRequest?,
        lastResult: BatchFetchResult<TData, TError>,
    ): BatchFetchResult<TData, TError> {
        val last = lastRequest.value
        requireNotNull(last)

        val req = if (lastResult is BatchFetchResult.Success) {
            if (lastResult.last && overrideRequest == null) {
                return BatchFetchResult.UnknownError(EndOfPaginationException())
            }

            Request(
                offset = last.offset + last.limit,
                limit = batchSize,
                request = overrideRequest ?: last.request,
            )
        } else {
            last
        }

        val res = fetch(req)
        lastRequest.value = req
        return res
    }
}