package com.tangem.domain.polymarket.model

import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchingContext
import com.tangem.pagination.PaginationStatus

typealias PolymarketActivityBatchingContext = BatchingContext<Int, PolymarketActivityListConfig, Nothing>
typealias PolymarketActivityBatchAction = BatchAction<Int, PolymarketActivityListConfig, Nothing>
typealias PolymarketActivityBatchFlow = BatchFlow<Int, List<PolymarketActivity>, Nothing>

data class PolymarketActivityListConfig(val addresses: PolymarketAddresses, val filter: PolymarketActivityFilter)

data class PolymarketActivityState(
    val items: List<PolymarketActivity> = emptyList(),
    val filter: PolymarketActivityFilter = PolymarketActivityFilter(),
    val status: PaginationStatus<List<PolymarketActivity>> = PaginationStatus.InitialLoading,
    val isRefreshing: Boolean = false,
)