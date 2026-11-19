package com.tangem.features.earn.api

interface EarnFeatureToggles {

    val isEarnEnabled: Boolean

    /** An Earn opportunity opens the user's holdings of the token before offering to add it. */
    val isUserPortfolioEnabled: Boolean

    val isGauntletVaultEnabled: Boolean
}