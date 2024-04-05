package com.tangem.data.source.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

// 🔥FIXME: Only logic to work with preferences must be here, must be separated to repositories
// TODO: Replace shared preferences with DataStore
@Deprecated("Create repository instead")
class PreferencesDataSource @Inject internal constructor(applicationContext: Context) {

    private val preferences: SharedPreferences =
        applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        incrementLaunchCounter()
    }

    private fun incrementLaunchCounter() {
        var count = preferences.getInt(APP_LAUNCH_COUNT_KEY, 0)
        preferences.edit { putInt(APP_LAUNCH_COUNT_KEY, ++count) }
    }

    companion object {
        private const val PREFERENCES_NAME = "tapPrefs"
        private const val APP_LAUNCH_COUNT_KEY = "launchCount"
    }
}