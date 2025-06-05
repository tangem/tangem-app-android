package com.tangem.data.txhistory.repository

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.txhistory.repository.paging.TxHistoryPageBatchFetcher
import com.tangem.datasource.local.txhistory.TxHistoryItemsStore
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.model.TxHistoryListBatchFlow
import com.tangem.domain.txhistory.model.TxHistoryListBatchingContext
import com.tangem.domain.txhistory.model.TxHistoryListConfig
import com.tangem.domain.txhistory.models.Page
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.walletmanager.utils.SdkPageConverter
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import timber.log.Timber

internal class RefactoredTxHistoryRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val txHistoryItemsStore: TxHistoryItemsStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
) : TxHistoryRepositoryV2 {

    private val sdkPageConverter = SdkPageConverter()
    private val TxHistoryListConfig.storeKey get() = TxHistoryItemsStore.Key(userWalletId, currency)

    override fun getTxHistoryBatchFlow(batchSize: Int, context: TxHistoryListBatchingContext): TxHistoryListBatchFlow {
        return BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: 0 },
            batchFetcher = createFetcher(batchSize),
        ).toBatchFlow()
    }

    private fun createFetcher(
        batchSize: Int,
    ): TxHistoryPageBatchFetcher<TxHistoryListConfig, PaginationWrapper<TxInfo>> =
        TxHistoryPageBatchFetcher { request, _ ->
            val wrappedItems = loadItems(request, batchSize)
            BatchFetchResult.Success(
                data = wrappedItems,
                empty = wrappedItems.items.isEmpty(),
                last = wrappedItems.nextPage is Page.LastPage,
            )
        }

    private suspend fun loadItems(
        request: TxHistoryPageBatchFetcher.Request<TxHistoryListConfig>,
        batchSize: Int,
    ): PaginationWrapper<TxInfo> {
        cacheRegistry.invokeOnExpire(
            key = getTxHistoryPageKey(request.page, request.params),
            skipCache = request.params.refresh,
            block = { fetch(request, batchSize) },
        )

        return txHistoryItemsStore.getSync(request.page, request.params)
    }

    private suspend fun fetch(request: TxHistoryPageBatchFetcher.Request<TxHistoryListConfig>, batchSize: Int) {
        val wrappedItems = walletManagersFacade.getTxHistoryItems(
            userWalletId = request.params.userWalletId,
            currency = request.params.currency,
            page = sdkPageConverter.convertBack(request.page),
            pageSize = batchSize,
        )

        txHistoryItemsStore.store(key = request.params.storeKey, value = wrappedItems)
    }

    private suspend fun TxHistoryItemsStore.getSync(
        pageToLoad: Page,
        config: TxHistoryListConfig,
    ): PaginationWrapper<TxInfo> {
        val storedItems = requireNotNull(getSyncOrNull(config.storeKey, pageToLoad)) {
            "The transaction history page #$pageToLoad could not be retrieved"
        }

        return if (pageToLoad is Page.Initial) storedItems.addRecentTransactions(config) else storedItems
    }

    private suspend fun PaginationWrapper<TxInfo>.addRecentTransactions(
        config: TxHistoryListConfig,
    ): PaginationWrapper<TxInfo> {
        val recentItems = walletManagersFacade.getRecentTransactions(
            userWalletId = config.userWalletId,
            currency = config.currency,
        )
            .filterUnconfirmedTransaction()
            .sortedByDescending { it.timestampInMillis }
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
                    transform = TxInfo::txHash,
                ),
            )

            return copy(items = recentItems + items)
        }
    }

    private fun List<TxInfo>.filterUnconfirmedTransaction(): List<TxInfo> {
        return filter { it.status == TxInfo.TransactionStatus.Unconfirmed }
    }

    private fun List<TxInfo>.filterIfTxAlreadyAdded(apiItems: List<TxInfo>): List<TxInfo> {
        return filter { item -> apiItems.none { it.txHash == item.txHash } }
    }

    private fun getTxHistoryPageKey(page: Page, config: TxHistoryListConfig): String {
        return "tx_history_page_${config.currency}_${config.userWalletId}_$page"
    }
}