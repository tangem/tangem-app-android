package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationOutcome
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObservePolymarketSessionKeyOutcomeUseCase(
    private val runner: PolymarketSessionKeyOperationRunner,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<PolymarketSessionKeyOperationOutcome?> =
        runner.observe(userWalletId).map { it.finished }.distinctUntilChanged()
}