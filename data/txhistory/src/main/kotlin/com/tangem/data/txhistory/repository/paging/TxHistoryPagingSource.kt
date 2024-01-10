package com.tangem.data.txhistory.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tangem.blockchain.common.Blockchain
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.datasource.local.txhistory.TxHistoryItemsStore
import com.tangem.domain.common.extensions.fromNetworkId
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

    override val keyReuseSupported: Boolean get() = true

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
        var wrappedItems = walletManagersFacade.getTxHistoryItems(
            userWalletId = sourceParams.userWalletId,
            currency = sourceParams.currency,
            page = pageToLoad,
            pageSize = pageSize,
        )

        if (pageToLoad == 1) {
            wrappedItems = wrappedItems.addRecentTransactions()
        }

        txHistoryItemsStore.store(key = storeKey, value = wrappedItems)
    }

    private suspend fun PaginationWrapper<TxHistoryItem>.addRecentTransactions(): PaginationWrapper<TxHistoryItem> {
        val recentTxHistoryItems = Blockchain.fromNetworkId(sourceParams.currency.network.backendId)?.let {
            walletManagersFacade.getRecentTransactions(
                userWalletId = sourceParams.userWalletId,
                blockchain = it,
                derivationPath = sourceParams.currency.network.derivationPath.value,
            )
                .filterUnconfirmedTransaction()
                .filterIfApiKnowsAboutTx(apiItems = items)
        } ?: emptyList()

        return if (recentTxHistoryItems.isEmpty()) {
            Timber.d("Nothing to add to TxHistory")
            this
        } else {
            Timber.d(
                "Recent transactions were added to TxHistory: %s",
                recentTxHistoryItems.joinToString(
                    prefix = "[",
                    postfix = "]",
                    transform = TxHistoryItem::txHash,
                ),
            )

            return copy(items = recentTxHistoryItems + items)
        }
    }

    private fun List<TxHistoryItem>.filterUnconfirmedTransaction(): List<TxHistoryItem> {
        return filter { it.status == TxHistoryItem.TransactionStatus.Unconfirmed }
    }

    private fun List<TxHistoryItem>.filterIfApiKnowsAboutTx(apiItems: List<TxHistoryItem>): List<TxHistoryItem> {
        return filter { item -> apiItems.none { it.txHash == item.txHash } }
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