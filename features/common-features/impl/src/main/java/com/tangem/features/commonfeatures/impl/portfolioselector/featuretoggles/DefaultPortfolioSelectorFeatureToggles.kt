package com.tangem.features.commonfeatures.impl.portfolioselector.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorFeatureToggles
import javax.inject.Inject

internal class DefaultPortfolioSelectorFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : PortfolioSelectorFeatureToggles {

    override val isSelectorV3Enabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1469_FOR_YOU_ENABLED)
}