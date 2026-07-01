package com.tangem.domain.assetsdiscovery.repository

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.assetsdiscovery.model.AssetsDiscoveryProgress
import kotlinx.coroutines.flow.Flow

interface AssetsDiscoveryRepository {

    suspend fun runDiscovery(userWalletId: UserWalletId)

    suspend fun completeDiscovery(userWalletId: UserWalletId)

    suspend fun getPendingDiscoveryWalletIds(): List<UserWalletId>

    fun observeDiscoveryProgress(userWalletId: UserWalletId): Flow<AssetsDiscoveryProgress>

    fun acknowledgeCompletion(userWalletId: UserWalletId)

    suspend fun clearPendingFlag(userWalletId: UserWalletId)

    suspend fun getDiscoveredCurrencies(userWalletId: UserWalletId): List<CryptoCurrency>

    suspend fun clearDiscoveredTokens(userWalletId: UserWalletId)

    suspend fun removeAppliedCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>)
}