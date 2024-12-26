package com.tangem.features.stories.impl

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.stories.api.StoriesFeatureToggles

internal class DefaultStoriesFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : StoriesFeatureToggles {
    override val isFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("STORIES_ENABLED")
}