package com.tangem.features.tangempay

interface TangemPayFeatureToggles {
    val isGoogleWalletPushProvisioningEnabled: Boolean
    val isGoogleWalletButtonAlwaysShown: Boolean
    val isOrderFrameworkEnabled: Boolean
}