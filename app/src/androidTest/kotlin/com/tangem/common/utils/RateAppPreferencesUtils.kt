package com.tangem.common.utils

import androidx.datastore.preferences.core.MutablePreferences
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import java.util.concurrent.TimeUnit

private const val FUNDS_FOUND_DAYS_AGO = 4L
private const val APP_LAUNCH_COUNT = 3

fun MutablePreferences.setRateAppReadyToShow() {
    val keys = StorePreferencesKeys()
    this[keys.fundsFoundDateKey] = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(FUNDS_FOUND_DAYS_AGO)
    this[keys.appLaunchCountKey] = APP_LAUNCH_COUNT
    this[keys.showRatingDialogAtLaunchCountKey] = APP_LAUNCH_COUNT
}