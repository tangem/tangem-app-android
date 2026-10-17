package com.tangem.features.tangempay

interface TangemPayFeatureToggles {
    val isPlasticCardOrderEnabled: Boolean
    val isBankOfframpEnabled: Boolean
    val isCustomCardsCobrandEnabled: Boolean
    val isResidencyStepEnabled: Boolean
    val isVisaOnrampV2Enabled: Boolean
    val isGoogleWalletPushProvisioningEnabled: Boolean
    val isGoogleWalletButtonAlwaysShown: Boolean
}