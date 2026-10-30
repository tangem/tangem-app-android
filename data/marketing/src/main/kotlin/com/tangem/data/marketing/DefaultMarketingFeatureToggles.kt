package com.tangem.data.marketing

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.marketing.MarketingFeatureToggles

internal class DefaultMarketingFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : MarketingFeatureToggles {

    override val isUidBindingEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1732_MARKETING_UID_BINDING_ENABLED)

    override val isPromoPageEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1732_PROMO_PAGE_ENABLED)
}