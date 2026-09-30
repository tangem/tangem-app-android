package com.tangem.features.storiesv2.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.storiesv2.StoriesV2FeatureToggles
import javax.inject.Inject

internal class DefaultStoriesV2FeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : StoriesV2FeatureToggles {

    override val isHardwareUpgradeStoriesAbEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1357_HARDWARE_UPGRADE_STORIES_AB_ENABLED)
}