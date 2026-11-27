package com.tangem.features.marketing.api

interface MarketingFeatureToggles {

    /**
     * Rollout flag of the Money account campaign (`tangem://onboard-virtual-account`). While off, the campaign is not
     * shown and the account status is not looked up for its button.
     */
    val isMoneyAccountBannerEnabled: Boolean
}