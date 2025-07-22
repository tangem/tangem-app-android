package com.tangem.data.txhistory.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.txhistory.repository.paging.TxHistoryPagingSource
import com.tangem.datasource.local.txhistory.TxHistoryItemsStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.models.Page
import com.tangem.domain.txhistory.models.TxHistoryState
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.walletmanager.utils.SdkPageConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

class DefaultTxHistoryRepository(
    private val cacheRegistry: CacheRegistry,
    private val walletManagersFacade: WalletManagersFacade,
    private val userWalletsStore: UserWalletsStore,
    private val txHistoryItemsStore: TxHistoryItemsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : TxHistoryRepository {
    private val sdkPageConverter by lazy { SdkPageConverter() }

    override suspend fun getTxHistoryItemsCount(userWalletId: UserWalletId, currency: CryptoCurrency): Int {
        return withContext(dispatchers.io) {
            val userWallet = getUserWallet(userWalletId)
            val state = walletManagersFacade.getTxHistoryState(
                userWalletId = userWallet.walletId,
                currency = currency,
            )

            when (state) {
                is TxHistoryState.Failed.FetchError -> throw TxHistoryStateError.DataError(state.exception)
                is TxHistoryState.NotImplemented -> throw TxHistoryStateError.TxHistoryNotImplemented
                is TxHistoryState.Success.Empty -> throw TxHistoryStateError.EmptyTxHistories
                is TxHistoryState.Success.HasTransactions -> state.txCount
            }
        }
    }

    override fun getTxHistoryItems(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        pageSize: Int,
        refresh: Boolean,
    ): Flow<PagingData<TxInfo>> {
        val pager = Pager(
            config = PagingConfig(
                pageSize = pageSize,
                initialLoadSize = pageSize,
            ),
            pagingSourceFactory = {
                TxHistoryPagingSource(
                    sourceParams = TxHistoryPagingSource.Params(userWalletId, currency, pageSize, refresh),
                    txHistoryItemsStore = txHistoryItemsStore,
                    walletManagersFacade = walletManagersFacade,
                    cacheRegistry = cacheRegistry,
                )
            },
        )

        return pager.flow
    }

    @Deprecated("Replace with getTxExploreUrl [UserWalletId, Network] instead")
    override fun getTxExploreUrl(txHash: String, networkId: Network.ID): String {
        val blockchain = networkId.toBlockchain()
        return when (val txExploreState = blockchain.getExploreTxUrl(txHash)) {
            is TxExploreState.Url -> txExploreState.url
            is TxExploreState.Unsupported -> ""
        }
    }

    override suspend fun getTxExploreUrl(userWalletId: UserWalletId, network: Network): String {
        val blockchain = network.toBlockchain()
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )
        val lastTxHash = walletManager?.wallet?.recentTransactions?.last()?.hash.orEmpty()
        return when (val txExploreState = blockchain.getExploreTxUrl(lastTxHash)) {
            is TxExploreState.Url -> txExploreState.url
            else -> ""
        }
    }

    override suspend fun getFixedSizeTxHistoryItems(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        pageSize: Int,
        refresh: Boolean,
    ): List<TxInfo> = withContext(dispatchers.io) {
        try {
            cacheRegistry.invokeOnExpire(
                key = getTxHistoryPageKey(currency, userWalletId, Page.Initial),
                skipCache = refresh,
                block = { fetchFixedSizeTxHistoryItems(userWalletId, currency, pageSize) },
            )
            val txs = txHistoryItemsStore.getSyncOrNull(
                key = TxHistoryItemsStore.Key(userWalletId, currency),
                page = Page.Initial,
            )?.items
            txs ?: emptyList()
        } catch (e: Throwable) {
            Timber.e(e, "Unable to load the transaction history for the requested page: ${Page.Initial}")
            emptyList()
        }
    }

    private fun getTxHistoryPageKey(currency: CryptoCurrency, userWalletId: UserWalletId, page: Page): String {
        return "tx_history_page_${currency}_${userWalletId}_$page"
    }

    private suspend fun fetchFixedSizeTxHistoryItems(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        pageSize: Int,
    ) {
        val wrappedItems = walletManagersFacade.getTxHistoryItems(
            userWalletId = userWalletId,
            currency = currency,
            page = sdkPageConverter.convertBack(Page.Initial),
            pageSize = pageSize,
        )

        txHistoryItemsStore.store(TxHistoryItemsStore.Key(userWalletId, currency), wrappedItems)
    }

    private fun getUserWallet(userWalletId: UserWalletId): UserWallet {
        return requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Unable to find user wallet with provided ID: $userWalletId"
        }
    }
}