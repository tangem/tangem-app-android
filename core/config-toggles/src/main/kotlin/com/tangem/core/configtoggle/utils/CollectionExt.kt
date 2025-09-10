package com.tangem.core.configtoggle.utils

import com.tangem.core.configtoggle.version.VersionAvailabilityContract

internal fun Map<String, String>.defineTogglesAvailability(appVersion: String?): Map<String, Boolean> {
    return if (appVersion == null) {
        mapValues { false }
    } else {
        mapValues { (_, version) ->
            VersionAvailabilityContract(currentVersion = appVersion, localVersion = version)
        }
    }
}