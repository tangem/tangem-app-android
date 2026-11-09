package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObservePolymarketSessionKeyOperationUseCase(
    private val runner: PolymarketSessionKeyOperationRunner,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<PolymarketSessionKeyOperation?> =
        runner.observe(userWalletId).map { it.running }.distinctUntilChanged()
}