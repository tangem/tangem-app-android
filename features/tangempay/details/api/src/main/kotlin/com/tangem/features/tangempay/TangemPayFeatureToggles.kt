package com.tangem.features.tangempay

interface TangemPayFeatureToggles {
    val isResidencyStepEnabled: Boolean
    val isVisaOnrampV2Enabled: Boolean
    val isGoogleWalletPushProvisioningEnabled: Boolean
    val isGoogleWalletButtonAlwaysShown: Boolean
    val isPayBundleIntroductionEnabled: Boolean
}