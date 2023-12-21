package com.tangem.domain.tokens.repository

import com.tangem.domain.wallets.models.UserWalletId

interface NetworksCompatibilityRepository {

    @Throws(IllegalArgumentException::class)
    suspend fun areSolanaTokensSupportedIfRelevant(networkId: String, userWalletId: UserWalletId): Boolean

    @Throws(IllegalArgumentException::class)
    suspend fun areTokensSupportedByNetwork(networkId: String, userWalletId: UserWalletId): Boolean

    @Throws(IllegalArgumentException::class)
    suspend fun isNetworkSupported(networkId: String, userWalletId: UserWalletId): Boolean
}
