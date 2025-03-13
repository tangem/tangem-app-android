package com.tangem.domain.txhistory.repository

import com.tangem.domain.txhistory.model.TxHistoryListBatchFlow
import com.tangem.domain.txhistory.model.TxHistoryListBatchingContext

interface TxHistoryRepositoryV2 {

    fun getTxHistoryBatchFlow(batchSize: Int, context: TxHistoryListBatchingContext): TxHistoryListBatchFlow
}