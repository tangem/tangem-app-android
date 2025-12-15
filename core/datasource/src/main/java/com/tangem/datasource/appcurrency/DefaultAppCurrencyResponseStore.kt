package com.tangem.datasource.appcurrency

import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import kotlinx.coroutines.flow.Flow

/**
 * Default implementation of [AppCurrencyResponseStore]
 *
 * @property appPreferencesStore app preferences store
 */
internal class DefaultAppCurrencyResponseStore(
    private val appPreferencesStore: AppPreferencesStore,
) : AppCurrencyResponseStore {

    override fun get(): Flow<CurrenciesResponse.Currency?> {
        return appPreferencesStore.getObject(PreferencesKeys.SELECTED_APP_CURRENCY_KEY)
    }

    override suspend fun getSyncOrNull(): CurrenciesResponse.Currency? {
        return appPreferencesStore.getObjectSyncOrNull<CurrenciesResponse.Currency>(
            PreferencesKeys.SELECTED_APP_CURRENCY_KEY,
        )
    }

    override suspend fun store(currency: CurrenciesResponse.Currency) {
        appPreferencesStore.storeObject(
            PreferencesKeys.SELECTED_APP_CURRENCY_KEY,
            currency,
        )
    }
}