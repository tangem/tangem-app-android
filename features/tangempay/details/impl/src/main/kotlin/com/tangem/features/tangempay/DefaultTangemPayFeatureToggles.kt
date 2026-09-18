package com.tangem.features.tangempay

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultTangemPayFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TangemPayFeatureToggles {
    override val isPlasticCardOrderEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1157_PLASTIC_CARD_ORDER_ENABLED)

    override val isBankOfframpEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_16842_TANGEM_PAY_BANK_OFFRAMP_ENABLED)

    override val isCustomCardsCobrandEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1727_CUSTOM_CARDS_COBRAND_ENABLED)

    override val isResidencyStepEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1771_PAY_RESIDENCY_STEP_ENABLED)
}