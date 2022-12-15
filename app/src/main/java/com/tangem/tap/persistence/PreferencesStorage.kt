package com.tangem.tap.persistence

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.datasource.api.common.MoshiConverter
import java.util.*

class PreferencesStorage(applicationContext: Application) {

    private val preferences: SharedPreferences =
        applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val appRatingLaunchObserver: AppRatingLaunchObserver
    val usedCardsPrefStorage: UsedCardsPrefStorage
    val fiatCurrenciesPrefStorage: FiatCurrenciesPrefStorage
    val disclaimerPrefStorage: DisclaimerPrefStorage
    val toppedUpWalletStorage: ToppedUpWalletStorage

    init {
        incrementLaunchCounter()
        appRatingLaunchObserver = AppRatingLaunchObserver(preferences, getCountOfLaunches())
        usedCardsPrefStorage = UsedCardsPrefStorage(preferences, MoshiConverter.INSTANCE)
        usedCardsPrefStorage.migrate()
        fiatCurrenciesPrefStorage = FiatCurrenciesPrefStorage(preferences, MoshiConverter.INSTANCE)
        fiatCurrenciesPrefStorage.migrate()
        disclaimerPrefStorage = DisclaimerPrefStorage(preferences)
        toppedUpWalletStorage = ToppedUpWalletStorage(preferences, MoshiConverter.INSTANCE)
    }

    var chatFirstLaunchTime: Long?
        get() = preferences.getLong(CHAT_FIRST_LAUNCH_KEY, 0).takeIf { it != 0L }
        set(value) = preferences.edit { putLong(CHAT_FIRST_LAUNCH_KEY, value ?: 0) }

    var shouldShowSaveUserWalletScreen: Boolean
        get() = preferences.getBoolean(SAVE_WALLET_DIALOG_SHOWN_KEY, true)
        set(value) = preferences.edit {
            putBoolean(SAVE_WALLET_DIALOG_SHOWN_KEY, value)
        }

    var shouldSaveUserWallets: Boolean
        get() = preferences.getBoolean(SAVE_USER_WALLETS_KEY, false)
        set(value) = preferences.edit {
            putBoolean(SAVE_USER_WALLETS_KEY, value)
        }

    var shouldSaveAccessCodes: Boolean
        get() = preferences.getBoolean(SAVE_ACCESS_CODES_KEY, false)
        set(value) = preferences.edit {
            putBoolean(SAVE_ACCESS_CODES_KEY, value)
        }

    var wasApplicationStopped: Boolean
        get() = preferences.getBoolean(APPLICATION_STOPPED_KEY, false)
        set(value) = preferences.edit {
            putBoolean(APPLICATION_STOPPED_KEY, value)
        }

    var shouldOpenWelcomeScreenOnResume: Boolean
        get() = preferences.getBoolean(OPEN_WELCOME_ON_RESUME_KEY, false)
        set(value) = preferences.edit {
            putBoolean(OPEN_WELCOME_ON_RESUME_KEY, value)
        }

    fun getCountOfLaunches(): Int = preferences.getInt(APP_LAUNCH_COUNT_KEY, 1)

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

    companion object {
        private const val PREFERENCES_NAME = "tapPrefs"
        private const val TWINS_ONBOARDING_SHOWN_KEY = "twinsOnboardingShown"
        private const val APP_LAUNCH_COUNT_KEY = "launchCount"
        private const val CHAT_FIRST_LAUNCH_KEY = "chatFirstLaunchKey"
        private const val SAVE_WALLET_DIALOG_SHOWN_KEY = "saveUserWalletShown"
        private const val SAVE_USER_WALLETS_KEY = "saveUserWallets"
        private const val SAVE_ACCESS_CODES_KEY = "saveAccessCodes"
        private const val APPLICATION_STOPPED_KEY = "applicationStopped"
        private const val OPEN_WELCOME_ON_RESUME_KEY = "openWelcomeOnResume"
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
