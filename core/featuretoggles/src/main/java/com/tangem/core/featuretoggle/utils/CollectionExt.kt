package com.tangem.core.featuretoggle.utils

import com.tangem.core.featuretoggle.comparator.VersionContract
import com.tangem.core.featuretoggle.models.FeatureToggleDTO

/** Returns a Map containing toggle name and toggle availability pairs using version contract [contract] */
internal fun List<FeatureToggleDTO>.associate(contract: VersionContract): Map<String, Boolean> {
    return associate { toggle ->
        Pair(
            first = toggle.name,
            second = contract(toggle.version),
        )
    }
}
