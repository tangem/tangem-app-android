package com.tangem.data.settings

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_TOKEN_SWAP_PROMO_CHANGELLY_SHOW_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_WALLET_SWAP_PROMO_CHANGELLY_SHOW_KEY
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.repositories.SwapPromoRepository
import kotlinx.coroutines.flow.Flow

/**
 * Repository for showing swap promo notification.
 */
class DefaultSwapPromoRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : SwapPromoRepository {
    override fun isReadyToShowWalletPromo(): Flow<Boolean> {
        return appPreferencesStore.get(IS_WALLET_SWAP_PROMO_CHANGELLY_SHOW_KEY, true)
    }

    override fun isReadyToShowTokenPromo(): Flow<Boolean> {
        return appPreferencesStore.get(IS_TOKEN_SWAP_PROMO_CHANGELLY_SHOW_KEY, true)
    }

    override suspend fun setNeverToShowWalletPromo() {
        appPreferencesStore.store(
            key = IS_WALLET_SWAP_PROMO_CHANGELLY_SHOW_KEY,
            value = false,
        )
    }

    override suspend fun setNeverToShowTokenPromo() {
        appPreferencesStore.store(
            key = IS_TOKEN_SWAP_PROMO_CHANGELLY_SHOW_KEY,
            value = false,
        )
    }
}