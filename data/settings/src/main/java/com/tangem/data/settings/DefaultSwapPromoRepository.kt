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

    override fun isReadyToShowToken(): Flow<Boolean> {
        return appPreferencesStore.get(IS_TOKEN_SWAP_PROMO_SHOW_KEY, true)
            .map { it && checkPromoPeriod() }
    }

    override suspend fun setNeverToShowWallet() {
        appPreferencesStore.store(
            key = IS_WALLET_SWAP_PROMO_SHOW_KEY,
            value = false,
        )
    }

    override suspend fun setNeverToShowToken() {
        appPreferencesStore.store(
            key = IS_TOKEN_SWAP_PROMO_SHOW_KEY,
            value = false,
        )
    }

    private suspend fun checkPromoPeriod(): Boolean {
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis
        calendar.set(END_YEAR_KEY, END_MONTH_KEY, END_DAY_KEY, 0, 0, 0)
        val endTime = calendar.timeInMillis

        val shouldShow = endTime - currentTime > 0
        if (!shouldShow) {
            setNeverToShowToken()
            setNeverToShowWallet()
        }
        return shouldShow
    }

    companion object {
        private const val END_DAY_KEY = 1
        private const val END_MONTH_KEY = 1 // February
        private const val END_YEAR_KEY = 2024
    }
}