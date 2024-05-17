package com.tangem.domain.settings.repositories

import kotlinx.coroutines.flow.Flow

interface PromoSettingsRepository {
    fun isReadyToShowWalletSwapPromo(): Flow<Boolean>

    fun isReadyToShowTokenSwapPromo(): Flow<Boolean>

    suspend fun setNeverToShowWalletSwapPromo()

    suspend fun setNeverToShowTokenSwapPromo()

    fun isReadyToShowWalletTravalaPromo(): Flow<Boolean>

    suspend fun setNeverToShowWalletTravalaPromo()
}
