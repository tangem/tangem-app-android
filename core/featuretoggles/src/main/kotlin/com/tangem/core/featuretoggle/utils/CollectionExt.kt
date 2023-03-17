package com.tangem.core.featuretoggle.utils

import com.tangem.core.featuretoggle.contract.VersionAvailabilityContract
import com.tangem.core.featuretoggle.storage.FeatureToggle

internal fun List<FeatureToggle>.associateToggles(currentVersion: String): Map<String, Boolean> {
    return associate { localToggle ->
        Pair(
            first = localToggle.name,
            second = VersionAvailabilityContract(currentVersion, localToggle.version),
        )
    }
}
