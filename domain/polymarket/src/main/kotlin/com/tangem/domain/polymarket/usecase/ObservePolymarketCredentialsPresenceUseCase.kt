package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import kotlinx.coroutines.flow.Flow

class ObservePolymarketCredentialsPresenceUseCase(
    private val credentialsStore: PolymarketCredentialsStore,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<Boolean> =
        credentialsStore.hasCredentials(userWalletId = userWalletId)
}