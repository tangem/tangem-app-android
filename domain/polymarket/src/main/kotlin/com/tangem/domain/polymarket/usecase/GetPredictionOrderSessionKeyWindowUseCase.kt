package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.PolymarketOrderSigningRouter
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStateService
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyTradingStore
import java.time.Duration

class GetPredictionOrderSessionKeyWindowUseCase(
    private val router: PolymarketOrderSigningRouter,
    private val sessionKeyStore: PolymarketSessionKeyStore,
    private val stateService: PolymarketSessionKeyStateService,
    private val tradingStore: PolymarketSessionKeyTradingStore,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Duration? {
        router.sessionKeySigningWindow(userWalletId = userWalletId)?.let { return it }
        val heldAddress = sessionKeyStore.heldAddress(userWalletId) ?: return null
        if (tradingStore.isRefused(userWalletId, heldAddress)) return null

        stateService.refreshAndAwait(userWalletId)
        return router.sessionKeySigningWindow(userWalletId = userWalletId)
    }
}