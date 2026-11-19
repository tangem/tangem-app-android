package com.tangem.features.tangempay

interface TangemPayFeatureToggles {
    val isGoogleWalletPushProvisioningEnabled: Boolean
    val isGoogleWalletButtonAlwaysShown: Boolean
    val isOrderFrameworkEnabled: Boolean
    val isCompactEmptyHistoryEnabled: Boolean
    val isCardActionsEnabled: Boolean
}