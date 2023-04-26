package com.tangem.store.preferences.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.store.preferences.model.CurrencyDM
import com.tangem.store.preferences.model.FiatCurrencyDM

/**
 * Created by Anton Zhilenkov on 18/04/2022.
 */
@Deprecated("Create repository instead")
class FiatCurrenciesPrefStorage internal constructor(
    private val preferences: SharedPreferences,
    private val converter: MoshiJsonConverter,
) {
    fun migrate() {
        preferences.edit(true) {
            remove(FIAT_CURRENCIES_KEY_OLD)
            remove(APP_CURRENCY_KEY_OLD)
        }
    }

    fun getAppCurrency(): FiatCurrencyDM? {
        val json = preferences.getString(APP_CURRENCY_KEY, "")
        if (json.isNullOrBlank()) return null

        return converter.fromJson(json)
    }

    fun saveAppCurrency(fiatCurrency: FiatCurrencyDM) {
        val json = converter.toJson(fiatCurrency)
        preferences.edit { putString(APP_CURRENCY_KEY, json) }
    }

    fun save(currencies: List<CurrencyDM>) {
        val json: String = converter.toJson(currencies)
        return preferences.edit().putString(FIAT_CURRENCIES_KEY, json).apply()
    }

    fun restore(): List<CurrencyDM> {
        val json = preferences.getString(FIAT_CURRENCIES_KEY, "")
        val type = converter.typedList(CurrencyDM::class.java)
        if (json.isNullOrBlank()) return emptyList()

        return converter.fromJson(json, type) ?: emptyList()
    }

    companion object {
        private const val FIAT_CURRENCIES_KEY_OLD = "fiatCurrencies"
        private const val APP_CURRENCY_KEY_OLD = "appCurrency"

        private const val FIAT_CURRENCIES_KEY = "fiatCurrencies_v2"
        private const val APP_CURRENCY_KEY = "appCurrency_v2"
    }
}
