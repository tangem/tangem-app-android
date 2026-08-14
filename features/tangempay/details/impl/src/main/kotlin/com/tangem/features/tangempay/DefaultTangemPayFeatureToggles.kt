package com.tangem.features.tangempay

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultTangemPayFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TangemPayFeatureToggles {
    override val isTiersPlusPlanEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_16041_VISA_TIERS_PLUS_PLAN)

    override val isCashbackEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1192_TANGEM_PAY_CASHBACK_ENABLED)

    override val isPlasticCardOrderEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1157_PLASTIC_CARD_ORDER_ENABLED)

    override val isAccountMultichainEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1684_ACCOUNT_MULTICHAIN_ENABLED)

    override val isPinBiometryGateEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15130_PAY_PIN_BIOMETRY_GATE_ENABLED)
}