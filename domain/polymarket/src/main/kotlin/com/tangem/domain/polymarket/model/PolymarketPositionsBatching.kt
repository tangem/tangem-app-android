package com.tangem.domain.polymarket.model

import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchingContext

typealias PolymarketPositionsBatchingContext =
    BatchingContext<Int, PolymarketPositionsListConfig, PolymarketPositionsUpdateRequest>

typealias PolymarketPositionsBatchAction =
    BatchAction<Int, PolymarketPositionsListConfig, PolymarketPositionsUpdateRequest>

typealias PolymarketPositionsBatchFlow = BatchFlow<Int, List<PolymarketPosition>, PolymarketPositionsUpdateRequest>

typealias PolymarketPositionsBatchListState = BatchListState<Int, List<PolymarketPosition>>