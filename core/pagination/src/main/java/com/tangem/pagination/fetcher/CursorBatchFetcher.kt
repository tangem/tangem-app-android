package com.tangem.pagination.fetcher

import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * First page: cursor = null
 * Next pages: cursor = cursorFromItem(lastItemOfPreviousPage)
 */
class CursorBatchFetcher<TRequestParams : Any, TItem : Any>(
    private val prefetchDistance: Int,
    private val batchSize: Int,
    private val subFetcher: SubFetcher<TRequestParams, TItem>,
    private val cursorFromItem: (TItem) -> String,
) : BatchFetcher<TRequestParams, List<TItem>> {

    data class Request<TRequestParams>(
        val limit: Int,
        val cursor: String?,
        val params: TRequestParams,
    )

    fun interface SubFetcher<TRequestParams : Any, TItem : Any> {
        suspend fun fetch(
            request: Request<TRequestParams>,
            lastResult: BatchFetchResult<List<TItem>>?,
            isFirstBatchFetching: Boolean,
        ): BatchFetchResult<List<TItem>>
    }

    private val lastRequest = MutableStateFlow<Request<TRequestParams>?>(null)

    override suspend fun fetchFirst(requestParams: TRequestParams): BatchFetchResult<List<TItem>> {
        val request = Request(
            cursor = null,
            limit = prefetchDistance,
            params = requestParams,
        )

        val result = runCatching {
            subFetcher.fetch(request = request, lastResult = null, isFirstBatchFetching = true)
        }.getOrElse {
            currentCoroutineContext().ensureActive()
            return BatchFetchResult.Error(it)
        }

        lastRequest.value = request
        return result
    }

    override suspend fun fetchNext(
        overrideRequestParams: TRequestParams?,
        lastResult: BatchFetchResult<List<TItem>>,
    ): BatchFetchResult<List<TItem>> {
        val lastRequest = requireNotNull(lastRequest.value) { "fetchFirst() must be called before fetchNext()" }

        if (lastResult is BatchFetchResult.Success && lastResult.last && overrideRequestParams == null) {
            return BatchFetchResult.Error(EndOfPaginationException())
        }

        val nextReq: Request<TRequestParams> =
            if (lastResult is BatchFetchResult.Success<List<TItem>>) {
                val items = lastResult.data
                if (items.isEmpty()) {
                    return BatchFetchResult.Error(EndOfPaginationException())
                }

                val nextCursor = cursorFromItem(items.last())

                Request(
                    cursor = nextCursor,
                    limit = batchSize,
                    params = overrideRequestParams ?: lastRequest.params,
                )
            } else {
                lastRequest.copy(limit = batchSize, params = overrideRequestParams ?: lastRequest.params)
            }

        val result = runCatching {
            subFetcher.fetch(request = nextReq, lastResult = lastResult, isFirstBatchFetching = false)
        }.getOrElse {
            currentCoroutineContext().ensureActive()
            return BatchFetchResult.Error(it)
        }

        this.lastRequest.value = nextReq
        return result
    }
}