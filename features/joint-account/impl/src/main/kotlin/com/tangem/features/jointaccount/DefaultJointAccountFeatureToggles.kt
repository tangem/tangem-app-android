package com.tangem.features.jointaccount

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultJointAccountFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : JointAccountFeatureToggles {

    override val isJointAccountCreationEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.TWI_1611_JOINT_ACCOUNT_ENABLED)
}