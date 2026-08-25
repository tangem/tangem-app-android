package com.tangem.lib.auth.session

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * Preference keys owned by the auth library. The name strings must stay stable — they address the same
 * stored cells as before these keys were moved out of the central `PreferencesKeys`.
 */
object AuthPreferenceKeys {

    val IS_DEVICE_REGISTERED_KEY by lazy { booleanPreferencesKey(name = "isDeviceRegistered") }

    /** Base64 `UserWalletId`s already registered with the Tangem Auth Service (`/auth/wallet`). */
    val REGISTERED_WALLET_IDS_KEY by lazy { stringSetPreferencesKey(name = "registeredWalletIds") }
}