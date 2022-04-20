package com.tangem.tap.persistence

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.tap.common.entities.TapCurrency.Companion.DEFAULT_FIAT_CURRENCY
import com.tangem.tap.common.redux.global.FiatCurrencyName
import java.util.*


class PreferencesStorage(applicationContext: Application) {

    private val preferences: SharedPreferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val appRatingLaunchObserver: AppRatingLaunchObserver
    val usedCardsPrefStorage: UsedCardsPrefStorage
    val fiatCurrenciesPrefStorage: FiatCurrenciesPrefStorage

    init {
        incrementLaunchCounter()
        appRatingLaunchObserver = AppRatingLaunchObserver(preferences, getCountOfLaunches())
        usedCardsPrefStorage = UsedCardsPrefStorage(preferences, MoshiJsonConverter.INSTANCE)
        usedCardsPrefStorage.migrate()
        fiatCurrenciesPrefStorage = FiatCurrenciesPrefStorage(preferences, MoshiJsonConverter.INSTANCE)
        fiatCurrenciesPrefStorage.migrate()
    }

    fun getAppCurrency(): FiatCurrencyName {
        return preferences.getString(APP_CURRENCY_KEY, DEFAULT_FIAT_CURRENCY)
            ?: DEFAULT_FIAT_CURRENCY
    }

    fun saveAppCurrency(fiatCurrencyName: FiatCurrencyName) {
        preferences.edit { putString(APP_CURRENCY_KEY, fiatCurrencyName) }
    }

    fun getCountOfLaunches(): Int = preferences.getInt(APP_LAUNCH_COUNT_KEY, 1)

    @Deprecated("Use UsedCardsPrefStorage instead")
    fun wasCardScannedBefore(cardId: String): Boolean {
        return usedCardsPrefStorage.wasScanned(cardId)
    }

    fun saveDisclaimerAccepted() {
        preferences.edit { putBoolean(DISCLAIMER_ACCEPTED_KEY, true) }
    }

    fun wasDisclaimerAccepted(): Boolean {
        return preferences.getBoolean(DISCLAIMER_ACCEPTED_KEY, false)
    }

    fun saveTwinsOnboardingShown() {
        preferences.edit { putBoolean(TWINS_ONBOARDING_SHOWN_KEY, true) }
    }

    fun wasTwinsOnboardingShown(): Boolean {
        return preferences.getBoolean(TWINS_ONBOARDING_SHOWN_KEY, false)
    }

    private fun incrementLaunchCounter() {
        var count = preferences.getInt(APP_LAUNCH_COUNT_KEY, 0)
        preferences.edit { putInt(APP_LAUNCH_COUNT_KEY, ++count) }
    }

    fun wasRestoreFundsWarningClosed(): Boolean {
        return preferences.getBoolean(RESTORE_FUNDS_CLOSED_KEY, false)
    }

    fun saveRestoreFundsWarningClosed() {
        preferences.edit { putBoolean(RESTORE_FUNDS_CLOSED_KEY, true) }
    }

    companion object {
        private const val PREFERENCES_NAME = "tapPrefs"
        private const val APP_CURRENCY_KEY = "appCurrency"
        private const val DISCLAIMER_ACCEPTED_KEY = "disclaimerAccepted"
        private const val TWINS_ONBOARDING_SHOWN_KEY = "twinsOnboardingShown"
        private const val APP_LAUNCH_COUNT_KEY = "launchCount"
        private const val RESTORE_FUNDS_CLOSED_KEY = "restoreFundsClosed"
    }

}

class AppRatingLaunchObserver(
    private val preferences: SharedPreferences,
    private val launchCounts: Int,
) {
    private val K_SHOW_RATING_AT_LAUNCH_COUNT = "showRatingDialogAtLaunchCount"
    private val K_FUNDS_FOUND_DATE = "fundsFoundDate"
    private val K_USER_WAS_INTERACT_WITH_RATING = "userWasInteractWithRating"

    private val FUNDS_FOUND_DATE_UNDEFINED = -1L
    private val deferShowing = 20
    private val firstShowing = 3
    private var fundsFoundDate: Calendar? = null

    init {
        val msWhenFundsWasFound = preferences.getLong(K_FUNDS_FOUND_DATE, FUNDS_FOUND_DATE_UNDEFINED)
        if (msWhenFundsWasFound != FUNDS_FOUND_DATE_UNDEFINED) {
            fundsFoundDate = Calendar.getInstance().apply { timeInMillis = msWhenFundsWasFound }
        }
    }

    fun foundWalletWithFunds() {
        if (fundsFoundDate != null) return

        fundsFoundDate = Calendar.getInstance()
        preferences.edit(true) {
            putLong(K_FUNDS_FOUND_DATE, fundsFoundDate!!.timeInMillis).apply()
            putInt(K_SHOW_RATING_AT_LAUNCH_COUNT, launchCounts + firstShowing)
        }
    }

    fun isReadyToShow(): Boolean {
        val fundsDate = fundsFoundDate ?: return false

        if (!userWasInteractWithRating()) {
            val diff = Calendar.getInstance().timeInMillis - fundsDate.timeInMillis
            val diffInDays = diff / (1000 * 60 * 60 * 24)
            return launchCounts >= getCounterOfNextShowing() && diffInDays >= firstShowing
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