package com.tangem.data.txhistory.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.datasource.local.txhistory.TxHistoryItemsStore
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import timber.log.Timber

internal class TxHistoryPagingSource(
    private val sourceParams: Params,
    private val txHistoryItemsStore: TxHistoryItemsStore,
    private val walletManagersFacade: WalletManagersFacade,
    private val cacheRegistry: CacheRegistry,
) : PagingSource<Int, TxHistoryItem>() {

    private val storeKey = TxHistoryItemsStore.Key(sourceParams.userWalletId, sourceParams.currency)

    override fun getRefreshKey(state: PagingState<Int, TxHistoryItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.inc() ?: anchorPage?.nextKey?.dec()
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TxHistoryItem> {
        val pageToLoad = params.key ?: INITIAL_PAGE

        return try {
            val wrappedItems = loadItems(
                pageToLoad = pageToLoad,
                pageSize = sourceParams.pageSize,
                refresh = sourceParams.refresh && params is LoadParams.Refresh,
            )

            val items = wrappedItems.items
            val prevPage = when {
                items.isEmpty() -> null
                pageToLoad > INITIAL_PAGE -> pageToLoad.dec()
                else -> null
            }
            val nextPage = when {
                items.isEmpty() -> INITIAL_PAGE
                pageToLoad < wrappedItems.totalPages -> pageToLoad.inc()
                else -> null
            }

            LoadResult.Page(items, prevKey = prevPage, nextKey = nextPage)
        } catch (e: Throwable) {
            Timber.e(e, "Unable to load the transaction history for the requested page: $pageToLoad")

            LoadResult.Error(e)
        }
    }

    private suspend fun loadItems(pageToLoad: Int, pageSize: Int, refresh: Boolean): PaginationWrapper<TxHistoryItem> {
        cacheRegistry.invokeOnExpire(
            key = getTxHistoryPageKey(pageToLoad),
            skipCache = refresh,
            block = { fetch(pageToLoad, pageSize) },
        )

        return requireNotNull(txHistoryItemsStore.getSyncOrNull(storeKey, pageToLoad)) {
            "The transaction history page #$pageToLoad could not be retrieved"
        }
    }

    private suspend fun fetch(pageToLoad: Int, pageSize: Int) {
        val wrappedItems = walletManagersFacade.getTxHistoryItems(
            userWalletId = sourceParams.userWalletId,
            currency = sourceParams.currency,
            page = pageToLoad,
            pageSize = pageSize,
        )

        txHistoryItemsStore.store(storeKey, wrappedItems)
    }

    private fun getTxHistoryPageKey(page: Int): String {
        return "tx_history_page_${sourceParams.currency}_${sourceParams.userWalletId}_$page"
    }

    data class Params(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val pageSize: Int,
        val refresh: Boolean,
    )

    private companion object {
        private const val INITIAL_PAGE = 1
    }
}