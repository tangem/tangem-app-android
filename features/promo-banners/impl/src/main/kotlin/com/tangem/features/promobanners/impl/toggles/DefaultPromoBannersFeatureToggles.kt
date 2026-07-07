package com.tangem.features.promobanners.impl.toggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.promobanners.api.toggles.PromoBannersFeatureToggles
import javax.inject.Inject

class DefaultPromoBannersFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : PromoBannersFeatureToggles {

    override val isCampaignsToggleEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            FeatureToggles.TWI_1637_CASHBACK_AND_REACTIVATION_CAMPAIGNS_ENABLED,
        )
}