package com.tangem.features.gacha.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.gacha.api.GachaFeatureToggles

internal class DefaultGachaFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : GachaFeatureToggles {

    override val isGachaEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1640_GACHA_MACHINE_INTEGRATION_PHASE_ONE_ENABLED,
        )
}