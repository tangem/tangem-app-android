package com.tangem.data.txhistory.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.tangem.data.txhistory.repository.paging.TxHistoryPagingSource
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryState
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import kotlinx.coroutines.flow.Flow

class DefaultTxHistoryRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val walletsStateHolder: WalletsStateHolder,
) : TxHistoryRepository {

    override suspend fun getTxHistoryItemsCount(networkId: Network.ID, derivationPath: String?): Int {
        val userWallet = requireNotNull(walletsStateHolder.userWalletsListManager?.selectedUserWalletSync) {
            "Selected wallet must not be null"
        }
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
        val userWallet = requireNotNull(walletsStateHolder.userWalletsListManager?.selectedUserWalletSync) {
            "Selected wallet must not be null"
        }
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
}
