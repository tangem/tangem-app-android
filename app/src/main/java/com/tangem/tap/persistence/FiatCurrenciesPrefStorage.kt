package com.tangem.tap.persistence

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.datasource.api.tangemTech.CurrenciesResponse
import com.tangem.tap.common.entities.FiatCurrency

/**
 * Created by Anton Zhilenkov on 18/04/2022.
 */
class FiatCurrenciesPrefStorage(
    private val preferences: SharedPreferences,
    private val converter: MoshiJsonConverter,
) {
    fun migrate() {
        preferences.edit(true) {
            remove(FIAT_CURRENCIES_KEY_OLD)
            remove(APP_CURRENCY_KEY_OLD)
        }
    }

    fun getAppCurrency(): FiatCurrency {
        val json = preferences.getString(APP_CURRENCY_KEY, "")
        if (json.isNullOrBlank()) return FiatCurrency.Default

        return converter.fromJson(json) ?: FiatCurrency.Default
    }

    fun saveAppCurrency(fiatCurrency: FiatCurrency) {
        val json = converter.toJson(fiatCurrency)
        preferences.edit { putString(APP_CURRENCY_KEY, json) }
    }

    fun save(currencies: List<CurrenciesResponse.Currency>) {
        val json: String = converter.toJson(currencies)
        return preferences.edit().putString(FIAT_CURRENCIES_KEY, json).apply()
    }

    fun restore(): List<CurrenciesResponse.Currency> {
        val json = preferences.getString(FIAT_CURRENCIES_KEY, "")
        val type = converter.typedList(CurrenciesResponse.Currency::class.java)
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
