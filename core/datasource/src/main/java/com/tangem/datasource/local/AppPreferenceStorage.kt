package com.tangem.datasource.local

/**
 * Application local storage
 *
 * @author Andrew Khokhlov on 08/02/2023
 */
@Deprecated(message = "Use AppPreferencesStore", level = DeprecationLevel.WARNING)
interface AppPreferenceStorage {

    /** Json config with feature toggles 'ToggleName: String - Availability: Boolean' */
    var featureToggles: String
}
