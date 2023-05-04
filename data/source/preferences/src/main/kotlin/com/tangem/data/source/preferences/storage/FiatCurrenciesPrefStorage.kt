package com.tangem.data.source.preferences.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.data.source.preferences.model.DataSourceCurrency
import com.tangem.data.source.preferences.model.DataSourceFiatCurrency

/**
[REDACTED_AUTHOR]
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

    fun getAppCurrency(): DataSourceFiatCurrency? {
        val json = preferences.getString(APP_CURRENCY_KEY, "")
        if (json.isNullOrBlank()) return null

        return converter.fromJson(json)
    }

    fun saveAppCurrency(fiatCurrency: DataSourceFiatCurrency) {
        val json = converter.toJson(fiatCurrency)
        preferences.edit { putString(APP_CURRENCY_KEY, json) }
    }

    fun save(currencies: List<DataSourceCurrency>) {
        val json: String = converter.toJson(currencies)
        return preferences.edit().putString(FIAT_CURRENCIES_KEY, json).apply()
    }

    fun restore(): List<DataSourceCurrency> {
        val json = preferences.getString(FIAT_CURRENCIES_KEY, "")
        val type = converter.typedList(DataSourceCurrency::class.java)
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