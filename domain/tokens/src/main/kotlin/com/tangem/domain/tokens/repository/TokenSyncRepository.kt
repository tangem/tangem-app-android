package com.tangem.domain.tokens.repository

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.tokensync.TokenSyncProgress
import kotlinx.coroutines.flow.Flow

interface TokenSyncRepository {

    suspend fun runSync(userWalletId: UserWalletId)

    suspend fun getPendingSyncWalletIds(): List<UserWalletId>

    fun observeSyncProgress(userWalletId: UserWalletId): Flow<TokenSyncProgress>

    fun acknowledgeCompletion(userWalletId: UserWalletId)

    suspend fun clearPendingFlag(userWalletId: UserWalletId)

    suspend fun getDiscoveredCurrencies(userWalletId: UserWalletId): List<CryptoCurrency>

    suspend fun clearDiscoveredTokens(userWalletId: UserWalletId)
}