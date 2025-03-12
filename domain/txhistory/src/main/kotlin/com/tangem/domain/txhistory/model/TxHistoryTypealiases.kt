package com.tangem.domain.txhistory.model

import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchingContext

typealias TxHistoryListBatchingContext = BatchingContext<Int, TxHistoryListConfig, Nothing>

typealias TxHistoryListBatchFlow = BatchFlow<Int, PaginationWrapper<TxHistoryItem>, Nothing>