package com.tangem.domain.polymarket.model

import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchingContext

typealias PolymarketEventsBatchingContext =
    BatchingContext<Int, PolymarketEventsListConfig, PolymarketEventsUpdateRequest>

typealias PolymarketEventsBatchAction = BatchAction<Int, PolymarketEventsListConfig, PolymarketEventsUpdateRequest>

typealias PolymarketEventsBatchFlow = BatchFlow<Int, PolymarketEventsBatch, PolymarketEventsUpdateRequest>

typealias PolymarketEventsBatchListState = BatchListState<Int, PolymarketEventsBatch>

typealias PolymarketSearchBatchingContext = BatchingContext<Int, PolymarketSearchConfig, Nothing>

typealias PolymarketSearchBatchFlow = BatchFlow<Int, List<PolymarketEvent>, Nothing>

typealias PolymarketSearchBatchListState = BatchListState<Int, List<PolymarketEvent>>