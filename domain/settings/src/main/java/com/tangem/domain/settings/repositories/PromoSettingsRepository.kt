package com.tangem.domain.settings.repositories

import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface PromoSettingsRepository {
    fun isReadyToShowWalletSwapPromo(): Flow<Boolean>

    fun isReadyToShowTokenSwapPromo(): Flow<Boolean>

    suspend fun setNeverToShowWalletSwapPromo()

    suspend fun setNeverToShowTokenSwapPromo()

    fun isReadyToShowRingPromo(userWalletId: UserWalletId): Flow<Boolean>

    suspend fun setNeverToShowRingPromo()
}