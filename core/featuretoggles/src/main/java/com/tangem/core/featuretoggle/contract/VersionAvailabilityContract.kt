package com.tangem.core.featuretoggle.contract

/**
 * Version contract to evaluate availability of feature toggle
 *
 * @author Andrew Khokhlov on 08/02/2023
 */
internal object VersionAvailabilityContract {

    private const val DISABLED_FEATURE_TOGGLE_VERSION = "undefined"

    /** Evaluate availability of feature toggles using [currentVersion] and [localVersion] */
    operator fun invoke(currentVersion: String, localVersion: String): Boolean {
        if (localVersion == DISABLED_FEATURE_TOGGLE_VERSION) return false
        val current = Version.create(currentVersion) ?: return false
        val local = Version.create(localVersion) ?: return false

        return current >= local
    }
}
