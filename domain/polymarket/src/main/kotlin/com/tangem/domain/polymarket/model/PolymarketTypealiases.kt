package com.tangem.domain.polymarket.model

import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchingContext

typealias PolymarketEventsBatchingContext = BatchingContext<Int, PolymarketEventsListConfig, Nothing>

typealias PolymarketEventsBatchFlow = BatchFlow<Int, List<PolymarketEvent>, Nothing>