package com.tangem.core.configtoggle.utils

import com.tangem.core.configtoggle.storage.ConfigToggle
import com.tangem.core.configtoggle.version.VersionAvailabilityContract

internal fun List<ConfigToggle>.associateToggles(currentVersion: String): Map<String, Boolean> {
    return associate { localToggle ->
        Pair(
            first = localToggle.name,
            second = VersionAvailabilityContract(currentVersion, localToggle.version),
        )
    }
}

internal fun Map<String, String>.defineTogglesAvailability(appVersion: String?): Map<String, Boolean> {
    return if (appVersion == null) {
        mapValues { false }
    } else {
        mapValues { (_, version) ->
            VersionAvailabilityContract(currentVersion = appVersion, localVersion = version)
        }
    }
}