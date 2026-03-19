package com.tangem.core.configtoggle.feature.provider

import com.tangem.core.configtoggle.FeatureToggles
import javax.inject.Inject

/**
 * Default implementation of [FeatureTogglesProvider] that reads from the generated [FeatureToggles] enum.
 */
internal class DefaultFeatureTogglesProvider @Inject constructor() : FeatureTogglesProvider {

    override fun getToggles(): Map<String, String> {
        return FeatureToggles.entries.associate { it.rawName to it.version }
    }
}