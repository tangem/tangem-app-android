package com.tangem.feature.referral.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.feature.referral.domain.MobileWalletPromoRepository
import javax.inject.Inject

internal class DefaultMobileWalletPromoRepository @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
) : MobileWalletPromoRepository {

    override suspend fun shouldShowMobileWalletPromo(): Boolean {
        return appPreferencesStore.getSyncOrDefault(key = SHOULD_SHOW_MOBILE_WALLET_PROMO_KEY, default = false)
    }

    override suspend fun setShouldShowMobileWalletPromo(value: Boolean) {
        appPreferencesStore.editData { preferences ->
            preferences[SHOULD_SHOW_MOBILE_WALLET_PROMO_KEY] = value
        }
    }

    private companion object {
        val SHOULD_SHOW_MOBILE_WALLET_PROMO_KEY = booleanPreferencesKey("should_show_mobile_wallet_promo")
    }
}