package com.tangem.tap.persistence

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.tap.common.entities.TapCurrency.Companion.DEFAULT_FIAT_CURRENCY
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.network.coinmarketcap.FiatCurrency


class PreferencesStorage(applicationContext: Application) {

    private val preferences: SharedPreferences by lazy {
        applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private val fiatCurrenciesAdapter: JsonAdapter<List<FiatCurrency>> by lazy {
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        val type = Types.newParameterizedType(List::class.java, FiatCurrency::class.java)
        moshi.adapter(type)
    }

    fun getAppCurrency(): FiatCurrencyName {
        return preferences.getString(APP_CURRENCY_KEY, DEFAULT_FIAT_CURRENCY)
                ?: DEFAULT_FIAT_CURRENCY
    }

    fun saveAppCurrency(fiatCurrencyName: FiatCurrencyName) {
        return preferences.edit().putString(APP_CURRENCY_KEY, fiatCurrencyName).apply()
    }

    fun getFiatCurrencies(): List<FiatCurrency>? {
        val json = preferences.getString(FIAT_CURRENCIES_KEY, "")
        return if (json.isNullOrBlank()) null else fiatCurrenciesAdapter.fromJson(json) as List<FiatCurrency>
    }

    fun saveFiatCurrencies(currencies: List<FiatCurrency>) {
        val json: String = fiatCurrenciesAdapter.toJson(currencies)
        return preferences.edit().putString(FIAT_CURRENCIES_KEY, json).apply()
    }

    fun isFirstLaunch(): Boolean {
        val isFirst = !preferences.contains(FIRST_LAUNCH_CHECK_KEY)
        if (isFirst) preferences.edit().putInt(FIRST_LAUNCH_CHECK_KEY, System.currentTimeMillis().toInt()).apply()
        return isFirst
    }

    fun saveScannedCardId(cardId: String) {
        val scannedCardsIds: String = restoreScannedCardIds()
        if (!scannedCardsIds.contains(cardId)) {
            preferences.edit().putString(SCANNED_CARDS_IDS_KEY, "$scannedCardsIds$cardId, ").apply()
        }
    }

    fun wasCardScannedBefore(cardId: String): Boolean {
        return restoreScannedCardIds().contains(cardId)
    }

    private fun restoreScannedCardIds(): String =
            preferences.getString(SCANNED_CARDS_IDS_KEY, "") ?: ""

    companion object {
        private const val PREFERENCES_NAME = "tapPrefs"
        private const val APP_CURRENCY_KEY = "appCurrency"
        private const val FIAT_CURRENCIES_KEY = "fiatCurrencies"
        private const val FIRST_LAUNCH_CHECK_KEY = "firstLaunchCheck"
        private const val SCANNED_CARDS_IDS_KEY = "scannedCardIds"
    }

}