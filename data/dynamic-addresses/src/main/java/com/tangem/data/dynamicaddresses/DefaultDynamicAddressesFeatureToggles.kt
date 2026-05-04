package com.tangem.data.dynamicaddresses

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.dynamicaddresses.DynamicAddressesFeatureToggles

internal class DefaultDynamicAddressesFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : DynamicAddressesFeatureToggles {

    override val isDynamicAddressesEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.DYNAMIC_ADDRESSES_ENABLED)
}