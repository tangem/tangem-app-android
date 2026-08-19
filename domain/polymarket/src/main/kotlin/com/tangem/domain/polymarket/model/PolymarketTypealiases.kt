package com.tangem.domain.polymarket.model

import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchingContext

typealias PolymarketEventsBatchingContext = BatchingContext<Int, PolymarketEventsListConfig, Nothing>

typealias PolymarketEventsBatchFlow = BatchFlow<Int, List<PolymarketEvent>, Nothing>

typealias PolymarketEventsBatchListState = BatchListState<Int, List<PolymarketEvent>>

typealias PolymarketSearchBatchingContext = BatchingContext<Int, PolymarketSearchConfig, Nothing>

typealias PolymarketSearchBatchFlow = BatchFlow<Int, List<PolymarketEvent>, Nothing>