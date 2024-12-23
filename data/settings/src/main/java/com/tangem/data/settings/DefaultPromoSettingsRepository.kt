package com.tangem.data.settings

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys.ADDED_WALLETS_WITH_RING_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_TOKEN_SWAP_PROMO_OKX_SHOW_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_WALLET_SWAP_PROMO_OKX_SHOW_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.SHOULD_SHOW_RING_PROMO_KEY
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.repositories.PromoSettingsRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Repository for showing swap promo notification.
 */
class DefaultPromoSettingsRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : PromoSettingsRepository {
    override fun isReadyToShowWalletSwapPromo(): Flow<Boolean> {
        return appPreferencesStore.get(IS_WALLET_SWAP_PROMO_OKX_SHOW_KEY, true)
    }

    override fun isReadyToShowTokenSwapPromo(): Flow<Boolean> {
        return appPreferencesStore.get(IS_TOKEN_SWAP_PROMO_OKX_SHOW_KEY, true)
    }

    override suspend fun setNeverToShowWalletSwapPromo() {
        appPreferencesStore.store(
            key = IS_WALLET_SWAP_PROMO_OKX_SHOW_KEY,
            value = false,
        )
    }

    override suspend fun setNeverToShowTokenSwapPromo() {
        appPreferencesStore.store(
            key = IS_TOKEN_SWAP_PROMO_OKX_SHOW_KEY,
            value = false,
        )
    }

    override fun isReadyToShowRingPromo(userWalletId: UserWalletId): Flow<Boolean> {
        return combine(
            flow = appPreferencesStore
                .get(key = ADDED_WALLETS_WITH_RING_KEY, default = emptySet())
                .map { userWalletId.stringValue in it },
            flow2 = appPreferencesStore.get(key = SHOULD_SHOW_RING_PROMO_KEY, default = true),
            transform = { isRingAdded, shouldShowRingPromo -> isRingAdded && shouldShowRingPromo },
        )
    }

    override suspend fun setNeverToShowRingPromo() {
        appPreferencesStore.store(key = SHOULD_SHOW_RING_PROMO_KEY, value = false)
    }
}