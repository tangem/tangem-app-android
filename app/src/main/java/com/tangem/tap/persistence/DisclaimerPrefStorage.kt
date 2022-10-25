package com.tangem.tap.persistence

import android.content.SharedPreferences
import androidx.core.content.edit

/**
* [REDACTED_AUTHOR]
 */
class DisclaimerPrefStorage(
    private val preferences: SharedPreferences,
) {

    var hasTangemTosAccepted: Boolean
        get() = preferences.getBoolean(KEY_TANGEM_TOS, false)
        set(value) = preferences.edit { putBoolean(KEY_TANGEM_TOS, value) }

    var hasSaltPayTosAccepted: Boolean
        get() = preferences.getBoolean(KEY_SALT_PAY_TOS, false)
        set(value) = preferences.edit { putBoolean(KEY_SALT_PAY_TOS, value) }

    companion object {
        private const val KEY_TANGEM_TOS = "tangem_tos_accepted"
        private const val KEY_SALT_PAY_TOS = "saltPay_tos_accepted"
    }
}
