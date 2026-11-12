package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRunner

class AcknowledgePolymarketSessionKeyOutcomeUseCase(
    private val runner: PolymarketSessionKeyOperationRunner,
) {

    operator fun invoke(userWalletId: UserWalletId, operationId: Long) {
        runner.acknowledge(userWalletId, operationId)
    }
}