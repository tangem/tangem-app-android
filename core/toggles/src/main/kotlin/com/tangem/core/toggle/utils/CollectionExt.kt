package com.tangem.core.toggle.utils

import com.tangem.core.toggle.storage.Toggle
import com.tangem.core.toggle.version.VersionAvailabilityContract

internal fun List<Toggle>.associateToggles(currentVersion: String): Map<String, Boolean> {
    return associate { localToggle ->
        Pair(
            first = localToggle.name,
            second = VersionAvailabilityContract(currentVersion, localToggle.version),
        )
    }
}
