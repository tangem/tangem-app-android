package com.tangem.domain.tangempay.repository

import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchFlow
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchingContext

interface TangemPayTxHistoryRepository {
    fun getTxHistoryBatchFlow(
        batchSize: Int,
        context: TangemPayTxHistoryListBatchingContext,
    ): TangemPayTxHistoryListBatchFlow
}