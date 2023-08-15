package com.tangem.domain.txhistory.repository

import androidx.paging.PagingData
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.models.TxHistoryItem
import kotlinx.coroutines.flow.Flow

interface TxHistoryRepository {

    @Throws(TxHistoryStateError::class)
    suspend fun getTxHistoryItemsCount(networkId: Network.ID, derivationPath: String?): Int

    @Throws(TxHistoryListError::class)
    fun getTxHistoryItems(
        networkId: Network.ID,
        derivationPath: String?,
        pageSize: Int,
    ): Flow<PagingData<TxHistoryItem>>
}