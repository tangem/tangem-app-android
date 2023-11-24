package com.tangem.datasource.local

/**
 * Application local storage
 *
[REDACTED_AUTHOR]
 */
@Deprecated(message = "Use AppPreferencesStore", level = DeprecationLevel.WARNING)
interface AppPreferenceStorage {

    /** Json config with feature toggles 'ToggleName: String - Availability: Boolean' */
    var featureToggles: String
}