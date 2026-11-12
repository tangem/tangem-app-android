package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketFastTradingState
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStateService
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObservePolymarketFastTradingUseCase(
    private val stateService: PolymarketSessionKeyStateService,
    private val runner: PolymarketSessionKeyOperationRunner,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<PolymarketFastTradingState> = combine(
        stateService.observe(userWalletId),
        runner.observe(userWalletId),
    ) { overview, operations ->
        PolymarketFastTradingState(
            overview = overview,
            operation = operations.running,
            finished = operations.finished,
        )
    }.distinctUntilChanged()
}