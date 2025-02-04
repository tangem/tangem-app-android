package com.tangem.data.promo

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys.SHOULD_SHOW_SWAP_STORIES_KEY
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.promo.PromoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class DefaultPromoRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : PromoRepository {

    override fun isReadyToShowWalletSwapPromo(): Flow<Boolean> {
        return flowOf(false) // Use it on new promo action
    }

    override fun isReadyToShowTokenSwapPromo(): Flow<Boolean> {
        return flowOf(false) // Use it on new promo action
    }

    override suspend fun setNeverToShowWalletSwapPromo() {
        // Use it on new promo action
    }

    override suspend fun setNeverToShowTokenSwapPromo() {
        // Use it on new promo action
    }

    override fun isReadyToShowSwapStories(): Flow<Boolean> {
        return appPreferencesStore.get(SHOULD_SHOW_SWAP_STORIES_KEY, true)
    }

    override suspend fun isReadyToShowSwapStoriesSync(): Boolean {
        return appPreferencesStore.getSyncOrDefault(SHOULD_SHOW_SWAP_STORIES_KEY, true)
    }

    override suspend fun setNeverToShowSwapStories() {
        appPreferencesStore.store(
            key = SHOULD_SHOW_SWAP_STORIES_KEY,
            value = false,
        )
    }
}