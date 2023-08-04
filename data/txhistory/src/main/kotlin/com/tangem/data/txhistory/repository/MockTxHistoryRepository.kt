package com.tangem.data.txhistory.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.tangem.data.txhistory.repository.paging.TxHistoryPagingSource
import com.tangem.domain.txhistory.model.TxHistoryItem
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import kotlinx.coroutines.flow.Flow

internal class MockTxHistoryRepository : TxHistoryRepository {

    override suspend fun getTxHistoryItemsCount(networkId: String, derivationPath: String): Int {
        return 0
    }

    override fun getTxHistoryItems(networkId: String, pageSize: Int): Flow<PagingData<TxHistoryItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
            ),
            pagingSourceFactory = { TxHistoryPagingSource() },
        ).flow
    }
}
