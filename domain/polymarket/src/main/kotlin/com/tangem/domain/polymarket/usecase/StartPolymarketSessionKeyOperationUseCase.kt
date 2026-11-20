package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRunner
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStart

class StartPolymarketSessionKeyOperationUseCase(
    private val runner: PolymarketSessionKeyOperationRunner,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        request: PolymarketSessionKeyOperationRequest,
    ): PolymarketSessionKeyOperationStart = runner.start(userWalletId, request)
}