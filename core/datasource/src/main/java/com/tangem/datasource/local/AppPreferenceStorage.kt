package com.tangem.datasource.local

/**
 * Application local storage
 *
 * @author Andrew Khokhlov on 08/02/2023
 */
interface AppPreferenceStorage {

    /** Json config with feature toggles 'ToggleName: String - Availability: Boolean' */
    var featureToggles: String
}
