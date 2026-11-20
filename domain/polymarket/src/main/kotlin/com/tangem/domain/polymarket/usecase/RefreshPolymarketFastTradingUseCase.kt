package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStateService

class RefreshPolymarketFastTradingUseCase(
    private val stateService: PolymarketSessionKeyStateService,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        stateService.refreshAndAwait(userWalletId)
    }
}