package com.tangem.pagination.fetcher

import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fetcher that uses limit and offset to fetch data.
 *
 * @param TRequestParams type of the request params.
 * @param TData type of the data.
 *
 * @property prefetchDistance number of items to fetch for the first batch.
 * @property batchSize size of the batch.
 * @property subFetcher function that fetches the data.
 */
class LimitOffsetBatchFetcher<TRequestParams : Any, TData>(
    private val prefetchDistance: Int,
    private val batchSize: Int,
    private val subFetcher: SubFetcher<TRequestParams, TData>,
) : BatchFetcher<TRequestParams, TData> {

    data class Request<TRequestParams>(
        val limit: Int,
        val offset: Int,
        val params: TRequestParams,
    )

    fun interface SubFetcher<TRequestParams : Any, TData> {
        suspend fun fetch(
            request: Request<TRequestParams>,
            lastResult: BatchFetchResult<TData>?,
            isFirstBatchFetching: Boolean,
        ): BatchFetchResult<TData>
    }

    private val lastRequest = MutableStateFlow<Request<TRequestParams>?>(null)

    override suspend fun fetchFirst(requestParams: TRequestParams): BatchFetchResult<TData> {
        val req = Request(
            offset = 0,
            limit = prefetchDistance,
            params = requestParams,
        )

        val res = runCatching {
            subFetcher.fetch(request = req, lastResult = null, isFirstBatchFetching = true)
        }.getOrElse {
            currentCoroutineContext().ensureActive()
            BatchFetchResult.Error(it)
        }

        lastRequest.value = req
        return res
    }

    override suspend fun fetchNext(
        overrideRequestParams: TRequestParams?,
        lastResult: BatchFetchResult<TData>,
    ): BatchFetchResult<TData> {
        val last = lastRequest.value
        requireNotNull(last)

        val req = if (lastResult is BatchFetchResult.Success) {
            if (lastResult.last && overrideRequestParams == null) {
                return BatchFetchResult.Error(EndOfPaginationException())
            }

            Request(
                offset = last.offset + last.limit,
                limit = batchSize,
                params = overrideRequestParams ?: last.params,
            )
        } else {
            last
        }

        val res = runCatching {
            subFetcher.fetch(request = req, lastResult = lastResult, isFirstBatchFetching = false)
        }.getOrElse {
            currentCoroutineContext().ensureActive()
            BatchFetchResult.Error(it)
        }

        lastRequest.value = req
        return res
    }
}