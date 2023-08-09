package com.tangem.data.txhistory.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.tangem.data.txhistory.repository.paging.TxHistoryPagingSource
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryState
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.flow.Flow

class DefaultTxHistoryRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val userWalletsStore: UserWalletsStore,
) : TxHistoryRepository {

    override suspend fun getTxHistoryItemsCount(networkId: Network.ID, derivationPath: String?): Int {
        val userWallet = getUserWallet()
        val state = walletManagersFacade.getTxHistoryState(
            userWalletId = userWallet.walletId,
            networkId = networkId,
            rawDerivationPath = derivationPath,
        )
        return when (state) {
            is TxHistoryState.Failed.FetchError -> throw TxHistoryStateError.DataError(state.exception)
            TxHistoryState.NotImplemented -> throw TxHistoryStateError.TxHistoryNotImplemented
            TxHistoryState.Success.Empty -> throw TxHistoryStateError.EmptyTxHistories
            is TxHistoryState.Success.HasTransactions -> state.txCount
        }
    }

    override fun getTxHistoryItems(
        networkId: Network.ID,
        derivationPath: String?,
        pageSize: Int,
    ): Flow<PagingData<TxHistoryItem>> {
        val userWallet = getUserWallet()
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
            ),
            pagingSourceFactory = {
                TxHistoryPagingSource(
                    loadPage = { page: Int, pageSize: Int ->
                        walletManagersFacade.getTxHistoryItems(
                            userWalletId = userWallet.walletId,
                            networkId = networkId,
                            rawDerivationPath = derivationPath,
                            page = page,
                            pageSize = pageSize,
                        )
                    },
                )
            },
        ).flow
    }

    private fun getUserWallet(): UserWallet = requireNotNull(userWalletsStore.selectedUserWalletOrNull) {
        "Selected wallet must not be null"
    }
}
