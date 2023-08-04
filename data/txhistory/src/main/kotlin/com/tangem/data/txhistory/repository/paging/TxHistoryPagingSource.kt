package com.tangem.data.txhistory.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tangem.data.txhistory.mock.MockTxHistoryItems
import com.tangem.domain.txhistory.model.TxHistoryItem

private const val INITIAL_PAGE = 1

internal class TxHistoryPagingSource : PagingSource<Int, TxHistoryItem>() {

    override fun getRefreshKey(state: PagingState<Int, TxHistoryItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(other = 1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(other = 1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TxHistoryItem> {
        val currentPage = params.key ?: INITIAL_PAGE
        return try {
// [REDACTED_TODO_COMMENT]
            // val result = txHistoryManager.getTxHistoryItems(
            //     networkId = networkId,
            //     derivationPath = derivationPath,
            //     page = currentPage,
            //     pageSize = params.loadSize,
            // )
            val result = MockTxHistoryItems.txHistoryItems

            LoadResult.Page(
                data = result,
                prevKey = if (currentPage > INITIAL_PAGE) currentPage.minus(1) else null,
// [REDACTED_TODO_COMMENT]
                nextKey = null,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
