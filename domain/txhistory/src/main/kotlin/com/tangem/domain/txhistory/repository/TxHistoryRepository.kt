package com.tangem.domain.txhistory.repository

import androidx.paging.PagingData
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.txhistory.models.TxHistoryItem
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
    ): Flow<PagingData<TxHistoryItem>>

    fun getTxExploreUrl(txHash: String, networkId: Network.ID): String
}
