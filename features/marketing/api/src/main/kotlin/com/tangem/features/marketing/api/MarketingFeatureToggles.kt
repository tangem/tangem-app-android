package com.tangem.features.marketing.api

interface MarketingFeatureToggles {

    /**
     * Android has no route for `tangem://onboard-virtual-account` until Pay ships it. While off, the Money account
     * campaign is not shown and the account status is not looked up for its button.
     */
    val isMoneyAccountBannerEnabled: Boolean
}