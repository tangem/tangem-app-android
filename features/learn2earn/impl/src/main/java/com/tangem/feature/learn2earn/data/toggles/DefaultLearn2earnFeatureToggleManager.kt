package com.tangem.feature.learn2earn.data.toggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

/**
* [REDACTED_AUTHOR]
 */
internal class DefaultLearn2earnFeatureToggleManager(
    private val featureTogglesManager: FeatureTogglesManager,
) : Learn2earnFeatureToggleManager {

    override val isLearn2earnEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "1INCH_LEARN_2_EARN_ENABLED")
}
