package com.tangem.data.source.preferences.storage

import android.content.SharedPreferences
import androidx.core.content.edit

/**
[REDACTED_AUTHOR]
 */
@Deprecated("Create repository instead")
class DisclaimerPrefStorage internal constructor(
    private val preferences: SharedPreferences,
) {

    fun accept(disclaimerKey: String) {
        preferences.edit { putBoolean(disclaimerKey, true) }
    }

    fun isAccepted(disclaimerKey: String): Boolean {
        return preferences.getBoolean(disclaimerKey, false)
    }
}