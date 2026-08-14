package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.model.PolymarketApiCredentials

/**
 * Reads the stored CLOB credentials of [userWalletId]. Local only: it never reaches the network and never
 * needs a signature, so a caller can use it to find out whether a signing session is required at all.
 */
class GetPolymarketApiCredentialsUseCase(
    private val credentialsStore: PolymarketCredentialsStore,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): PolymarketApiCredentials? =
        credentialsStore.get(userWalletId = userWalletId)
}