package com.tangem.domain.polymarket.interactor

import arrow.core.raise.Raise
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyProgress
import kotlinx.coroutines.flow.FlowCollector

internal class SessionKeyOperationScope(
    raise: Raise<PolymarketSessionKeyLifecycleError>,
    collector: FlowCollector<PolymarketSessionKeyProgress>,
) : Raise<PolymarketSessionKeyLifecycleError> by raise,
    FlowCollector<PolymarketSessionKeyProgress> by collector