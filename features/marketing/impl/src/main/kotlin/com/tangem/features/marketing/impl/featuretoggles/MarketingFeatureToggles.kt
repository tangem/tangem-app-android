package com.tangem.features.marketing.impl.featuretoggles

internal interface MarketingFeatureToggles {

    /** Android has no route for `tangem://onboard-virtual-account` until Pay ships it, so the banner stays hidden */
    val isMoneyAccountBannerEnabled: Boolean
}