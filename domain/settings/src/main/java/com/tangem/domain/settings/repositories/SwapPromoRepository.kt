package com.tangem.domain.settings.repositories

import kotlinx.coroutines.flow.Flow

interface SwapPromoRepository {
    fun isReadyToShowWallet(): Flow<Boolean>

    fun isReadyToShowToken(): Flow<Boolean>

    suspend fun setNeverToShowWallet()

    suspend fun setNeverToShowToken()
}