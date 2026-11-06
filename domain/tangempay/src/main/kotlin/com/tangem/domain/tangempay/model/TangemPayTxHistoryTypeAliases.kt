package com.tangem.domain.tangempay.model

import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchingContext

typealias TangemPayTxHistoryListBatchingContext = BatchingContext<Int, TangemPayTxHistoryListConfig, Nothing>

typealias TangemPayTxHistoryListBatchFlow = BatchFlow<Int, List<TangemPayTxHistoryItem>, Nothing>