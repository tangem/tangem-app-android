package com.tangem.features.marketing.impl.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultMarketingFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : MarketingFeatureToggles {

    override val isMoneyAccountBannerEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_18957_MONEY_ACCOUNT_BANNER_ENABLED)
}