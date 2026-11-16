package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.PolymarketOrderSigningRouter
import java.time.Duration

class GetPredictionOrderSessionKeyWindowUseCase(
    private val router: PolymarketOrderSigningRouter,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Duration? =
        router.sessionKeySigningWindow(userWalletId = userWalletId)
}