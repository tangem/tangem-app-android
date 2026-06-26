package com.tangem.data.marketing.featuretoggle

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.marketing.MarketingFeatureToggles

internal class DefaultMarketingFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : MarketingFeatureToggles {

    override val isMarketingBannersEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15862_MARKETING_BANNERS_ENABLED)
}