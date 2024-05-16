package com.tangem.data.settings

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_TOKEN_SWAP_PROMO_CHANGELLY_SHOW_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_WALLET_SWAP_PROMO_CHANGELLY_SHOW_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_WALLET_TRAVALA_PROMO_SHOWN_KEY
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.repositories.PromoSettingsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Repository for showing swap promo notification.
 */
class DefaultPromoSettingsRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : PromoSettingsRepository {
    override fun isReadyToShowWalletSwapPromo(): Flow<Boolean> {
        return appPreferencesStore.get(IS_WALLET_SWAP_PROMO_CHANGELLY_SHOW_KEY, true)
    }

    override fun isReadyToShowTokenSwapPromo(): Flow<Boolean> {
        return appPreferencesStore.get(IS_TOKEN_SWAP_PROMO_CHANGELLY_SHOW_KEY, true)
    }

    override suspend fun setNeverToShowWalletSwapPromo() {
        appPreferencesStore.store(
            key = IS_WALLET_SWAP_PROMO_CHANGELLY_SHOW_KEY,
            value = false,
        )
    }

    override suspend fun setNeverToShowTokenSwapPromo() {
        appPreferencesStore.store(
            key = IS_TOKEN_SWAP_PROMO_CHANGELLY_SHOW_KEY,
            value = false,
        )
    }

    override fun isReadyToShowWalletTravalaPromo(): Flow<Boolean> {
        return appPreferencesStore.get(IS_WALLET_TRAVALA_PROMO_SHOWN_KEY, true)
    }

    override suspend fun setNeverToShowWalletTravalaPromo() {
        appPreferencesStore.store(
            key = IS_WALLET_TRAVALA_PROMO_SHOWN_KEY,
            value = false,
        )
    }
}
