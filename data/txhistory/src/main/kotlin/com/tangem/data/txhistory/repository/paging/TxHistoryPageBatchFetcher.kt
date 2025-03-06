package com.tangem.data.txhistory.repository.paging

import com.tangem.domain.txhistory.models.Page
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.exception.EndOfPaginationException
import com.tangem.pagination.fetcher.BatchFetcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow

internal class TxHistoryPageBatchFetcher<TRequestParams : Any, TData : PaginationWrapper<TxHistoryItem>>(
    private val subFetcher: SubFetcher<TRequestParams, TData>,
) : BatchFetcher<TRequestParams, TData> {
    data class Request<TRequestParams>(val page: Page, val params: TRequestParams)
    fun interface SubFetcher<TRequestParams : Any, TData> {
        suspend fun fetch(
            request: Request<TRequestParams>,
            lastResult: BatchFetchResult<TData>?,
        ): BatchFetchResult<TData>
    }

    private val lastRequest = MutableStateFlow<Request<TRequestParams>?>(null)

    override suspend fun fetchFirst(requestParams: TRequestParams): BatchFetchResult<TData> {
        val req = Request(
            page = Page.Initial,
            params = requestParams,
        )

        val res = runCatching {
            subFetcher.fetch(request = req, lastResult = null)
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
                page = lastResult.data.nextPage,
                params = overrideRequestParams ?: last.params,
            )
        } else {
            last
        }

        val res = runCatching {
            subFetcher.fetch(request = req, lastResult = lastResult)
        }.getOrElse {
            currentCoroutineContext().ensureActive()
            BatchFetchResult.Error(it)
        }

        lastRequest.value = req
        return res
    }
}