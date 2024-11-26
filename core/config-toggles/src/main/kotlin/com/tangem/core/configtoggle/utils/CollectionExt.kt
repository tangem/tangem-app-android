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