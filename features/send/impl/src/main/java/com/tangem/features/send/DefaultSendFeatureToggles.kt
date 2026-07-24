package com.tangem.features.send

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.send.api.SendFeatureToggles
import javax.inject.Inject

internal class DefaultSendFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : SendFeatureToggles {

    override val isHighFeeWarningEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1367_HIGH_FEE_WARNING_ENABLED,
        )
}