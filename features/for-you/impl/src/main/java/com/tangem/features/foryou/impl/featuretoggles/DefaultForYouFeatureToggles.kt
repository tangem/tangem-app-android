package com.tangem.features.foryou.impl.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.foryou.ForYouFeatureToggles
import javax.inject.Inject

internal class DefaultForYouFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : ForYouFeatureToggles {
    override val isForYouEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1469_FOR_YOU_ENABLED)
}