package com.tangem.datasource.local

/**
 * Application local storage
 *
[REDACTED_AUTHOR]
 */
interface AppPreferenceStorage {

    /** Json config with feature toggles 'ToggleName: String - Availability: Boolean' */
    var featureToggles: String
}