package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore

class InvalidatePolymarketCredentialsUseCase(
    private val credentialsStore: PolymarketCredentialsStore,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        credentialsStore.clear(userWalletId = userWalletId)
    }
}