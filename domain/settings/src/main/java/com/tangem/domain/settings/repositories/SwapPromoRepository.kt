package com.tangem.domain.settings.repositories

import kotlinx.coroutines.flow.Flow

interface SwapPromoRepository {
    fun isReadyToShowWalletPromo(): Flow<Boolean>

    fun isReadyToShowTokenPromo(): Flow<Boolean>

    suspend fun setNeverToShowWalletPromo()

    suspend fun setNeverToShowTokenPromo()
}