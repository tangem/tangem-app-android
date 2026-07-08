package com.tangem.data.promo.store

import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.TokenReward

interface PromoEnrollmentStore {

    suspend fun getSyncOrNull(campaign: PromoCampaignId): TokenReward?

    suspend fun store(campaign: PromoCampaignId, tokenReward: TokenReward)
}