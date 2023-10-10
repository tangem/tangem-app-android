package com.tangem.data.txhistory.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.txhistory.repository.paging.TxHistoryPagingSource
import com.tangem.datasource.local.txhistory.TxHistoryItemsStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryState
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

class DefaultTxHistoryRepository(
    private val cacheRegistry: CacheRegistry,
    private val walletManagersFacade: WalletManagersFacade,
    private val userWalletsStore: UserWalletsStore,
    private val txHistoryItemsStore: TxHistoryItemsStore,
) : TxHistoryRepository {

    override suspend fun getTxHistoryItemsCount(userWalletId: UserWalletId, network: Network): Int {
        val userWallet = getUserWallet(userWalletId)
        val state = walletManagersFacade.getTxHistoryState(
            userWalletId = userWallet.walletId,
            network = network,
        )
        return when (state) {
            is TxHistoryState.Failed.FetchError -> throw TxHistoryStateError.DataError(state.exception)
            is TxHistoryState.NotImplemented -> throw TxHistoryStateError.TxHistoryNotImplemented
            is TxHistoryState.Success.Empty -> throw TxHistoryStateError.EmptyTxHistories
            is TxHistoryState.Success.HasTransactions -> state.txCount
        }
    }

    override fun getTxHistoryItems(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        pageSize: Int,
        refresh: Boolean,
    ): Flow<PagingData<TxHistoryItem>> {
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

    private suspend fun getUserWallet(userWalletId: UserWalletId): UserWallet {
        return requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Unable to find user wallet with provided ID: $userWalletId"
        }
    }
}
