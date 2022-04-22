package com.tangem.tap.persistence

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.network.api.tangemTech.CurrenciesResponse

/**
[REDACTED_AUTHOR]
 */
class FiatCurrenciesPrefStorage(
    private val preferences: SharedPreferences,
    private val converter: MoshiJsonConverter,
) {
    private val FIAT_CURRENCIES_KEY_OLD = "fiatCurrencies"
    private val FIAT_CURRENCIES_KEY = "fiatCurrencies_v2"

    fun migrate() {
        preferences.edit(true) {
            remove(FIAT_CURRENCIES_KEY_OLD)
        }
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
}