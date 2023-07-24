package com.tangem.data.txhistory.repository

import com.tangem.data.txhistory.mock.MockTxHistoryItems
import com.tangem.domain.txhistory.model.TxHistoryItem
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MockTxHistoryRepository : TxHistoryRepository {

    override suspend fun getTxHistoryItemsCount(networkId: String, derivationPath: String): Int {
        return 0
    }

    override fun getTxHistoryItems(
        networkId: String,
        derivationPath: String,
        page: Int,
        pageSize: Int,
    ): Flow<List<TxHistoryItem>> {
        return flowOf(MockTxHistoryItems.txHistoryItems)
    }
}
