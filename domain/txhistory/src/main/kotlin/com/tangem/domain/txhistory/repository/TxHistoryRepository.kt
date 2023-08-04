package com.tangem.domain.txhistory.repository

import androidx.paging.PagingData
import com.tangem.domain.txhistory.error.TxHistoryListError
import com.tangem.domain.txhistory.error.TxHistoryStateError
import com.tangem.domain.txhistory.model.TxHistoryItem
import kotlinx.coroutines.flow.Flow

interface TxHistoryRepository {

    @Throws(TxHistoryStateError::class)
    suspend fun getTxHistoryItemsCount(networkId: String, derivationPath: String): Int

    @Throws(TxHistoryListError::class)
    fun getTxHistoryItems(networkId: String, pageSize: Int): Flow<PagingData<TxHistoryItem>>
}
