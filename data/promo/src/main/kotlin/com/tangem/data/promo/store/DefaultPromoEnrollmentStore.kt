package com.tangem.data.promo.store

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.TokenReward

internal class DefaultPromoEnrollmentStore(
    private val appPreferencesStore: AppPreferencesStore,
) : PromoEnrollmentStore {

    override suspend fun getSyncOrNull(campaign: PromoCampaignId): TokenReward? {
        return appPreferencesStore
            .getObjectMapSync<TokenReward>(PreferencesKeys.PROMO_ENROLLMENTS_KEY)[campaign.slug]
    }

    override suspend fun store(campaign: PromoCampaignId, tokenReward: TokenReward) {
        appPreferencesStore.editData { mutablePreferences ->
            val current = mutablePreferences.getObjectMap<TokenReward>(PreferencesKeys.PROMO_ENROLLMENTS_KEY)
            mutablePreferences.setObjectMap(
                key = PreferencesKeys.PROMO_ENROLLMENTS_KEY,
                value = current + (campaign.slug to tokenReward),
            )
        }
    }
}