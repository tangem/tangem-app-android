package com.tangem.domain.txhistory.repository

import androidx.paging.PagingData
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface TxHistoryRepository {

    @Throws(TxHistoryStateError::class)
    suspend fun getTxHistoryItemsCount(userWalletId: UserWalletId, currency: CryptoCurrency): Int

    @Throws(TxHistoryListError::class)
    fun getTxHistoryItems(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        pageSize: Int,
        refresh: Boolean,
    ): Flow<PagingData<TxInfo>>

    fun getTxExploreUrl(txHash: String, networkId: Network.ID): String

    /** Get transaction url in explorer via last transaction from wallet's recentTransactions list */
    suspend fun getTxExploreUrl(userWalletId: UserWalletId, network: Network): String

    @Throws(TxHistoryListError::class)
    suspend fun getFixedSizeTxHistoryItems(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        pageSize: Int,
        refresh: Boolean,
    ): List<TxInfo>
}