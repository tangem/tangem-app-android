package com.tangem.data.txhistory.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem

private const val INITIAL_PAGE = 1

internal class TxHistoryPagingSource(
    private val loadPage: suspend (page: Int, pageSize: Int) -> PaginationWrapper<TxHistoryItem>,
) : PagingSource<Int, TxHistoryItem>() {

    override fun getRefreshKey(state: PagingState<Int, TxHistoryItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(other = 1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(other = 1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TxHistoryItem> {
        val currentPage = params.key ?: INITIAL_PAGE
        return try {
            val result = loadPage(currentPage, params.loadSize)

            LoadResult.Page(
                data = result.items,
                prevKey = if (currentPage > INITIAL_PAGE) currentPage.minus(1) else null,
                nextKey = if (result.page < result.totalPages) currentPage.plus(1) else null,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}