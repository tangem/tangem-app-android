package com.tangem.core.ui.components.background.northernlights

import android.content.Context
import androidx.core.content.edit

class NorthernLightsCrashGuardPreferences(context: Context) : NorthernLightsCrashGuard.Store {

    // The project's DataStore wrapper is suspend-only and journals asynchronously
    // a fast crash would beat the write and the guard would silently never trip.
    // This needs a synchronous barrier, so it's a dedicated prefs file with commit()
    // and a @Suppress("ApplySharedPref")
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun load(): NorthernLightsCrashGuard.State = NorthernLightsCrashGuard.State(
        armedVersion = preferences.getInt(KEY_ARMED_VERSION, NO_VERSION),
        crashCount = preferences.getInt(KEY_CRASH_COUNT, 0),
        crashVersion = preferences.getInt(KEY_CRASH_VERSION, NO_VERSION),
    )

    @Suppress("ApplySharedPref")
    override fun save(state: NorthernLightsCrashGuard.State) {
        preferences.edit(commit = true) {
            putInt(KEY_ARMED_VERSION, state.armedVersion)
                .putInt(KEY_CRASH_COUNT, state.crashCount)
                .putInt(KEY_CRASH_VERSION, state.crashVersion)
        }
    }

    private companion object {
        const val FILE_NAME = "northern_lights_crash_guard"
        const val KEY_ARMED_VERSION = "armed_version"
        const val KEY_CRASH_COUNT = "crash_count"
        const val KEY_CRASH_VERSION = "crash_version"
        const val NO_VERSION = -1
    }
}