package com.tangem.features.promobanners.impl.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.promobanners.api.PromoBannersFeatureToggles

internal class DefaultPromoBannersFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : PromoBannersFeatureToggles {

    override val isPromoCampaignsV2Enabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1705_PROMO_CAMPAIGNS_V2_ENABLED)
}