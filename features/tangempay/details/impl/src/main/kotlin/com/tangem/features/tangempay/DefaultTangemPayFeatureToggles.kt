package com.tangem.features.tangempay

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.utils.annotations.RemoveWithToggle

internal class DefaultTangemPayFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TangemPayFeatureToggles {
    override val isResidencyStepEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1771_PAY_RESIDENCY_STEP_ENABLED)

    override val isVisaOnrampV2Enabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1753_VISA_ONRAMP_V2_ENABLED)

    override val isGoogleWalletPushProvisioningEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1062_GOOGLE_WALLET_PUSH_PROVISIONING_ENABLED)

    @RemoveWithToggle(
        toggleName = "TWI_1062_GOOGLE_WALLET_PUSH_PROVISIONING_ENABLED",
        description = "debug switch, drop TWI_1062_GOOGLE_WALLET_BUTTON_ALWAYS_SHOWN with the interface property",
    )
    override val isGoogleWalletButtonAlwaysShown: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1062_GOOGLE_WALLET_BUTTON_ALWAYS_SHOWN)
}