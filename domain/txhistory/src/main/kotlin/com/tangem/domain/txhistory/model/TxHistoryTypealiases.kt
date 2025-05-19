package com.tangem.domain.txhistory.model

import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchingContext

typealias TxHistoryListBatchingContext = BatchingContext<Int, TxHistoryListConfig, Nothing>

typealias TxHistoryListBatchFlow = BatchFlow<Int, PaginationWrapper<TxInfo>, Nothing>