package com.tangem.domain.polymarket.model

import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchingContext

typealias PolymarketPositionsBatchingContext = BatchingContext<Int, PolymarketPositionsListConfig, Nothing>

typealias PolymarketPositionsBatchAction = BatchAction<Int, PolymarketPositionsListConfig, Nothing>

typealias PolymarketPositionsBatchFlow = BatchFlow<Int, List<PolymarketPosition>, Nothing>

typealias PolymarketPositionsBatchListState = BatchListState<Int, List<PolymarketPosition>>