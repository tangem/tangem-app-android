package com.tangem.core.configtoggle.version

/**
 * Version contract to evaluate availability of feature toggle
 *
[REDACTED_AUTHOR]
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