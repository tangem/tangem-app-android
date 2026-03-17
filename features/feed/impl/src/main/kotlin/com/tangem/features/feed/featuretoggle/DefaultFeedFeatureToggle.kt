package com.tangem.features.feed.featuretoggle

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.feed.entry.featuretoggle.FeedFeatureToggle

internal class DefaultFeedFeatureToggle(
    private val featureTogglesManager: FeatureTogglesManager,
) : FeedFeatureToggle {

    override val isEarnBlockEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.EARN_BLOCK_ENABLED)
}