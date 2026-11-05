package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.PolymarketOrderSigningRouter

class CanSignPredictionOrderWithSessionKeyUseCase(
    private val router: PolymarketOrderSigningRouter,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Boolean =
        router.canSignWithSessionKey(userWalletId = userWalletId)
}