package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.model.PolymarketApiCredentials

class InvalidatePolymarketCredentialsUseCase(
    private val credentialsStore: PolymarketCredentialsStore,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, rejected: PolymarketApiCredentials) {
        credentialsStore.clearIfCurrent(userWalletId = userWalletId, rejected = rejected)
    }
}