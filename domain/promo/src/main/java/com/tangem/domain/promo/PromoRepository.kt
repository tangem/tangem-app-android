package com.tangem.domain.promo

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoId
import com.tangem.domain.promo.models.StoryContent
import kotlinx.coroutines.flow.Flow

interface PromoRepository {

    // region Promo
    fun isReadyToShowWalletPromo(userWalletId: UserWalletId, promoId: PromoId): Flow<Boolean>

    fun isReadyToShowTokenPromo(promoId: PromoId): Flow<Boolean>

    suspend fun setNeverToShowWalletPromo(promoId: PromoId)

    suspend fun setNeverToShowTokenPromo(promoId: PromoId)

    suspend fun isMarketsStakingNotificationHideClicked(): Flow<Boolean>

    suspend fun setMarketsStakingNotificationHideClicked()
    // endregion

    // region Stories
    fun getStoryById(id: String): Flow<StoryContent?>

    suspend fun getStoryByIdSync(id: String, refresh: Boolean): StoryContent?

    fun isReadyToShowStories(storyId: String): Flow<Boolean>

    suspend fun isReadyToShowStoriesSync(storyId: String): Boolean

    suspend fun setNeverToShowStories(storyId: String)
    // endregion
}