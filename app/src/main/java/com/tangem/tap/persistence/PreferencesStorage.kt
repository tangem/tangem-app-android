package com.tangem.tap.persistence

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.tap.common.entities.TapCurrency.Companion.DEFAULT_FIAT_CURRENCY
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.network.coinmarketcap.FiatCurrency
import java.util.*


class PreferencesStorage(applicationContext: Application) {

    private val preferences: SharedPreferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val appRatingLaunchObserver: AppRatingLaunchObserver

    init {
        incrementLaunchCounter()
        appRatingLaunchObserver = AppRatingLaunchObserver(preferences, getCountOfLaunches())
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

    fun getCountOfLaunches(): Int = preferences.getInt(APP_LAUNCH_COUNT_KEY, 1)

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

    fun saveDisclaimerAccepted() {
        preferences.edit().putBoolean(DISCLAIMER_ACCEPTED_KEY, true).apply()
    }

    fun wasDisclaimerAccepted(): Boolean {
        return preferences.getBoolean(DISCLAIMER_ACCEPTED_KEY, false)
    }

    fun saveTwinsOnboardingShown() {
        preferences.edit().putBoolean(TWINS_ONBOARDING_SHOWN_KEY, true).apply()
    }

    fun wasTwinsOnboardingShown(): Boolean {
        return preferences.getBoolean(TWINS_ONBOARDING_SHOWN_KEY, false)
    }

    private fun incrementLaunchCounter() {
        var count = preferences.getInt(APP_LAUNCH_COUNT_KEY, 0)
        preferences.edit { putInt(APP_LAUNCH_COUNT_KEY, ++count) }
    }

    companion object {
        private const val PREFERENCES_NAME = "tapPrefs"
        private const val APP_CURRENCY_KEY = "appCurrency"
        private const val FIAT_CURRENCIES_KEY = "fiatCurrencies"
        private const val SCANNED_CARDS_IDS_KEY = "scannedCardIds"
        private const val DISCLAIMER_ACCEPTED_KEY = "disclaimerAccepted"
        private const val TWINS_ONBOARDING_SHOWN_KEY = "twinsOnboardingShown"
        private const val APP_LAUNCH_COUNT_KEY = "launchCount"
    }

}

class AppRatingLaunchObserver(
        private val preferences: SharedPreferences,
        private val launchCounts: Int,
) {
    private val K_SHOW_RATING_AT_LAUNCH_COUNT = "showRatingDialogAtLaunchCount"
    private val K_FUNDS_FOUND_DATE = "fundsFoundDate"
    private val K_USER_WAS_INTERACT_WITH_RATING = "userWasInteractWithRating"

    private val deferShowing = 20
    private val firstShowing = 3
    private var fundsFoundDate: Calendar? = null

    init {
        val dateTimeMs = preferences.getLong(K_FUNDS_FOUND_DATE, -1)
        if (dateTimeMs > 0) fundsFoundDate = Calendar.getInstance().apply { timeInMillis = dateTimeMs }
    }

    fun foundWalletWithFunds() {
        if (fundsFoundDate != null) return

        fundsFoundDate = Calendar.getInstance()
        preferences.edit().putLong(K_FUNDS_FOUND_DATE, fundsFoundDate!!.timeInMillis).apply()
    }

    fun isReadyToShow(): Boolean {
        val fundsDate = fundsFoundDate ?: return false

        if (!userWasInteractWithRating()) {
            val diff = Calendar.getInstance().timeInMillis - fundsDate.timeInMillis
            val diffInDays = diff / (100 * 60 * 60 * 24)
            if (diffInDays >= firstShowing) return true
        }

        val nextShowing = getCounterOfNextShowing()
        return launchCounts >= nextShowing
    }

    fun applyDelayedShowing() {
        updateNextShowing(launchCounts + deferShowing)
    }

    fun setNeverToShow() {
        updateNextShowing(999999999)
    }

    private fun updateNextShowing(at: Int) {
        val editor = preferences.edit()
        editor.putInt(K_SHOW_RATING_AT_LAUNCH_COUNT, at)
        editor.putBoolean(K_USER_WAS_INTERACT_WITH_RATING, true)
        editor.apply()
    }

    private fun userWasInteractWithRating(): Boolean = preferences.getBoolean(K_USER_WAS_INTERACT_WITH_RATING, false)
    private fun getCounterOfNextShowing(): Int = preferences.getInt(K_SHOW_RATING_AT_LAUNCH_COUNT, firstShowing)
}