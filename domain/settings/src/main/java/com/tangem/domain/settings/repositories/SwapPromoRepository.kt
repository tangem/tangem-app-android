package com.tangem.domain.settings.repositories

import kotlinx.coroutines.flow.Flow

interface SwapPromoRepository {
    fun isReadyToShowWallet(): Flow<Boolean>

    fun isReadyToShowToken(userWalletId: String, currencyId: String): Flow<Boolean>

    suspend fun setNeverToShowWallet()

    suspend fun setNeverToShowToken(userWalletId: String, currencyId: String)
}