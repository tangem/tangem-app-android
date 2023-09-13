package com.tangem.domain.txhistory.repository

import androidx.paging.PagingData
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import kotlinx.coroutines.flow.Flow

interface TxHistoryRepository {

    @Throws(TxHistoryStateError::class)
    suspend fun getTxHistoryItemsCount(network: Network): Int

    @Throws(TxHistoryListError::class)
    fun getTxHistoryItems(network: Network, pageSize: Int): Flow<PagingData<TxHistoryItem>>
}