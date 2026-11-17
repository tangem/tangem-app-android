package com.tangem.domain.polymarket.model

import arrow.core.Either
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationOutcome

data class PolymarketFastTradingState(
    val overview: Either<PolymarketWalletError, PolymarketSessionKeyOverview>,
    val operation: PolymarketSessionKeyOperation?,
    val finished: PolymarketSessionKeyOperationOutcome?,
)