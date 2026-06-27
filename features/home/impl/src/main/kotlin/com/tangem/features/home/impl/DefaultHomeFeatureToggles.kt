package com.tangem.features.home.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.home.api.HomeFeatureToggles

internal class DefaultHomeFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : HomeFeatureToggles {

    override val isStoriesContainerEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15901_STORIES_CONTAINER_ENABLED)
}