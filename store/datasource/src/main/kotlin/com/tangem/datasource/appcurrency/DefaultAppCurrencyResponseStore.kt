package com.tangem.datasource.appcurrency

import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
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
        return appPreferencesStore.getObject(SELECTED_APP_CURRENCY_KEY)
    }

    override suspend fun getSyncOrNull(): CurrenciesResponse.Currency? {
        return appPreferencesStore.getObjectSyncOrNull<CurrenciesResponse.Currency>(SELECTED_APP_CURRENCY_KEY)
    }

    override suspend fun store(currency: CurrenciesResponse.Currency) {
        appPreferencesStore.storeObject(SELECTED_APP_CURRENCY_KEY, currency)
    }

    companion object {
        // Keep the persisted name "selectedAppCurrency" so values survive the store moving out of core:datasource.
        internal val SELECTED_APP_CURRENCY_KEY = stringPreferencesKey(name = "selectedAppCurrency")
    }
}