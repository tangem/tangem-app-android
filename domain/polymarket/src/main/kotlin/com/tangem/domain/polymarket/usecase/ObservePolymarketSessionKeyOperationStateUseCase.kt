package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRunner
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationState
import kotlinx.coroutines.flow.Flow

class ObservePolymarketSessionKeyOperationStateUseCase(
    private val runner: PolymarketSessionKeyOperationRunner,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<PolymarketSessionKeyOperationState> =
        runner.observe(userWalletId)
}