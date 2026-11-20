package com.tangem.features.feed

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultFeedFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : FeedFeatureToggles {

    override val isDiscoverDeeplinkFiltersEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_19355_DISCOVER_DEEPLINK_FILTERS_ENABLED)
}