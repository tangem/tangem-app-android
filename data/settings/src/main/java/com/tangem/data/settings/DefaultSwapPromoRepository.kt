package com.tangem.data.settings

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_TOKEN_SWAP_PROMO_SHOW_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_WALLET_SWAP_PROMO_SHOW_KEY
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.repositories.SwapPromoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * Repository for showing swap promo notification.
 */
class DefaultSwapPromoRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : SwapPromoRepository {
    override fun isReadyToShowWallet(): Flow<Boolean> {
        return appPreferencesStore.get(IS_WALLET_SWAP_PROMO_SHOW_KEY, true)
            .map { it && checkPromoPeriod() }
    }

    override fun isReadyToShowToken(userWalletId: String, currencyId: String): Flow<Boolean> {
        return appPreferencesStore.get(IS_TOKEN_SWAP_PROMO_SHOW_KEY, emptySet())
            .map { !it.contains(userWalletId + currencyId) && checkPromoPeriod() }
    }

    override suspend fun setNeverToShowWallet() {
        appPreferencesStore.store(
            key = IS_WALLET_SWAP_PROMO_SHOW_KEY,
            value = false,
        )
    }

    override suspend fun setNeverToShowToken(userWalletId: String, currencyId: String) {
        appPreferencesStore.editData { mutablePreferences ->
            val storesCurrencies = mutablePreferences.getOrDefault(IS_TOKEN_SWAP_PROMO_SHOW_KEY, emptySet())
            mutablePreferences.set(
                key = IS_TOKEN_SWAP_PROMO_SHOW_KEY,
                value = storesCurrencies.toMutableSet().apply { add(userWalletId + currencyId) },
            )
        }
    }

    private fun checkPromoPeriod(): Boolean {
        val currentDate = Calendar.getInstance()
        return currentDate.get(Calendar.DATE) in START_DATE_KEY..END_DATE_KEY &&
            currentDate.get(Calendar.MONTH) == MONTH_KEY && currentDate.get(Calendar.YEAR) == YEAR_KEY
    }

    companion object {
        private const val START_DATE_KEY = 15
        private const val END_DATE_KEY = 31
        private const val MONTH_KEY = 11 // December
        private const val YEAR_KEY = 2023 // just in case
    }
}