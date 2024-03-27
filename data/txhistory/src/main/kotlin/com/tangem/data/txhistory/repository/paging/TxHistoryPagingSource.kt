package com.tangem.data.txhistory.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.datasource.local.txhistory.TxHistoryItemsStore
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.txhistory.models.Page
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.walletmanager.utils.SdkPageConverter
import com.tangem.domain.wallets.models.UserWalletId
import timber.log.Timber

internal class TxHistoryPagingSource(
    private val sourceParams: Params,
    private val txHistoryItemsStore: TxHistoryItemsStore,
    private val walletManagersFacade: WalletManagersFacade,
    private val cacheRegistry: CacheRegistry,
) : PagingSource<Page, TxHistoryItem>() {

    private val storeKey = TxHistoryItemsStore.Key(sourceParams.userWalletId, sourceParams.currency)
    private val sdkPageConverter by lazy { SdkPageConverter() }

    override val keyReuseSupported: Boolean get() = true

    override fun getRefreshKey(state: PagingState<Page, TxHistoryItem>): Page? {
        return null
    }

    override suspend fun load(params: LoadParams<Page>): LoadResult<Page, TxHistoryItem> {
        val pageToLoad = params.key ?: Page.Initial

        return try {
            val wrappedItems = loadItems(
                pageToLoad = pageToLoad,
                pageSize = sourceParams.pageSize,
                refresh = sourceParams.refresh && params is LoadParams.Refresh,
            )
            val nextKey = when (wrappedItems.nextPage) {
                Page.LastPage -> null
                Page.Initial, is Page.Next -> wrappedItems.nextPage
            }
            LoadResult.Page(wrappedItems.items, prevKey = null, nextKey = nextKey)
        } catch (e: Throwable) {
            Timber.e(e, "Unable to load the transaction history for the requested page: $pageToLoad")

            LoadResult.Error(e)
        }
    }

    private suspend fun loadItems(pageToLoad: Page, pageSize: Int, refresh: Boolean): PaginationWrapper<TxHistoryItem> {
        cacheRegistry.invokeOnExpire(
            key = getTxHistoryPageKey(pageToLoad),
            skipCache = refresh,
            block = { fetch(pageToLoad, pageSize) },
        )

        return txHistoryItemsStore.getSync(pageToLoad)
    }

    private suspend fun fetch(pageToLoad: Page, pageSize: Int) {
        val wrappedItems = walletManagersFacade.getTxHistoryItems(
            userWalletId = sourceParams.userWalletId,
            currency = sourceParams.currency,
            page = sdkPageConverter.convertBack(pageToLoad),
            pageSize = pageSize,
        )

        txHistoryItemsStore.store(key = storeKey, value = wrappedItems)
    }

    private suspend fun TxHistoryItemsStore.getSync(pageToLoad: Page): PaginationWrapper<TxHistoryItem> {
        val storedItems = requireNotNull(getSyncOrNull(storeKey, pageToLoad)) {
            "The transaction history page #$pageToLoad could not be retrieved"
        }

        return if (pageToLoad is Page.Initial) storedItems.addRecentTransactions() else storedItems
    }

    private suspend fun PaginationWrapper<TxHistoryItem>.addRecentTransactions(): PaginationWrapper<TxHistoryItem> {
        val recentItems = walletManagersFacade.getRecentTransactions(
            userWalletId = sourceParams.userWalletId,
            currency = sourceParams.currency,
        )
            .filterUnconfirmedTransaction()
            .filterIfTxAlreadyAdded(apiItems = items)

        return if (recentItems.isEmpty()) {
            Timber.d("Nothing to add to TxHistory")
            this
        } else {
            Timber.d(
                "Recent transactions were added to TxHistory: %s",
                recentItems.joinToString(
                    prefix = "[",
                    postfix = "]",
                    transform = TxHistoryItem::txHash,
                ),
            )

            return copy(items = recentItems + items)
        }
    }

    private fun List<TxHistoryItem>.filterUnconfirmedTransaction(): List<TxHistoryItem> {
        return filter { it.status == TxHistoryItem.TransactionStatus.Unconfirmed }
    }

    private fun List<TxHistoryItem>.filterIfTxAlreadyAdded(apiItems: List<TxHistoryItem>): List<TxHistoryItem> {
        return filter { item -> apiItems.none { it.txHash == item.txHash } }
    }

    private fun getTxHistoryPageKey(page: Page): String {
        return "tx_history_page_${sourceParams.currency}_${sourceParams.userWalletId}_$page"
    }

    data class Params(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val pageSize: Int,
        val refresh: Boolean,
    )
}
