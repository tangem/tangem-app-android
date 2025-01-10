package com.tangem.domain.promo

import com.tangem.domain.promo.models.PromoBanner
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface PromoRepository {

    suspend fun getChangellyPromoBanner(): PromoBanner?

    suspend fun getOkxPromoBanner(): PromoBanner?

    suspend fun getRingPromoBanner(): PromoBanner?

    fun isReadyToShowWalletSwapPromo(): Flow<Boolean>

    fun isReadyToShowTokenSwapPromo(): Flow<Boolean>

    suspend fun setNeverToShowWalletSwapPromo()

    suspend fun setNeverToShowTokenSwapPromo()

    fun isReadyToShowRingPromo(userWalletId: UserWalletId): Flow<Boolean>

    suspend fun setNeverToShowRingPromo()

    fun isReadyToShowSwapStories(): Flow<Boolean>

    suspend fun isReadyToShowSwapStoriesSync(): Boolean

    suspend fun setNeverToShowSwapStories()
}