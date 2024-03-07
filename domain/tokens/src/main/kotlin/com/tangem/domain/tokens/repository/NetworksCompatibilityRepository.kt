package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

interface NetworksCompatibilityRepository {

    @Throws(IllegalArgumentException::class)
    suspend fun areSolanaTokensSupportedIfRelevant(networkId: String, userWalletId: UserWalletId): Boolean

    @Throws(IllegalArgumentException::class)
    suspend fun areTokensSupportedByNetwork(networkId: String, userWalletId: UserWalletId): Boolean

    @Throws(IllegalArgumentException::class)
    suspend fun isNetworkSupported(networkId: String, userWalletId: UserWalletId): Boolean

    @Throws(IllegalArgumentException::class)
    suspend fun getSupportedNetworks(userWalletId: UserWalletId): List<Network>

    suspend fun requiresHardenedDerivationOnly(networkId: String, userWalletId: UserWalletId): Boolean

    fun areTokensSupportedByNetwork(networkId: String): Boolean
}