package com.tangem.domain.promo

import kotlinx.coroutines.flow.Flow

interface PromoRepository {

    fun isReadyToShowWalletSwapPromo(): Flow<Boolean>

    fun isReadyToShowTokenSwapPromo(): Flow<Boolean>

    suspend fun setNeverToShowWalletSwapPromo()

    suspend fun setNeverToShowTokenSwapPromo()

    fun isReadyToShowSwapStories(): Flow<Boolean>

    suspend fun isReadyToShowSwapStoriesSync(): Boolean

    suspend fun setNeverToShowSwapStories()
}