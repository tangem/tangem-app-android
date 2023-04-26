package com.tangem.data.source.preferences

import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.*

@Deprecated("Create repository instead")
class AppRatingLaunchObserver internal constructor(
    private val preferences: SharedPreferences,
    private val launchCounts: Int,
) {

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
            val diffInDays = diff / DAY_MILLIS
            return launchCounts >= getCounterOfNextShowing() && diffInDays >= firstShowing
        }

        val nextShowing = getCounterOfNextShowing()
        return launchCounts >= nextShowing
    }

    fun applyDelayedShowing() {
        updateNextShowing(launchCounts + deferShowing)
    }

    fun setNeverToShow() {
        updateNextShowing(Int.MAX_VALUE)
    }

    private fun updateNextShowing(at: Int) {
        val editor = preferences.edit()
        editor.putInt(K_SHOW_RATING_AT_LAUNCH_COUNT, at)
        editor.putBoolean(K_USER_WAS_INTERACT_WITH_RATING, true)
        editor.apply()
    }

    private fun userWasInteractWithRating(): Boolean = preferences.getBoolean(K_USER_WAS_INTERACT_WITH_RATING, false)

    private fun getCounterOfNextShowing(): Int = preferences.getInt(K_SHOW_RATING_AT_LAUNCH_COUNT, firstShowing)

    companion object {
        private const val K_SHOW_RATING_AT_LAUNCH_COUNT = "showRatingDialogAtLaunchCount"
        private const val K_FUNDS_FOUND_DATE = "fundsFoundDate"
        private const val K_USER_WAS_INTERACT_WITH_RATING = "userWasInteractWithRating"
        private const val FUNDS_FOUND_DATE_UNDEFINED = -1L
        private const val DAY_MILLIS = 1000 * 60 * 60 * 24
    }
}