package com.tangem.features.feed.featuretoggle

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.feed.entry.featuretoggle.FeedFeatureToggle

internal class DefaultFeedFeatureToggle(
    private val featureTogglesManager: FeatureTogglesManager,
) : FeedFeatureToggle {

    override val isFeedEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("FEED_ENABLED")

    override val isEarnBlockEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("EARN_BLOCK_ENABLED")
}