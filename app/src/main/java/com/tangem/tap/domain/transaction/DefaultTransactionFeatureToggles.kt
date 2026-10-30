package com.tangem.tap.domain.transaction

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.transaction.TransactionFeatureToggles

internal class DefaultTransactionFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TransactionFeatureToggles {

    override val isTronClientEnergyEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.TWI_1858_TRON_CLIENT_ENERGY_ENABLED)

    override val isTronLowerFeeTypeEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.AND_17201_TRON_LOWER_FEE_TYPE_ENABLED)
}