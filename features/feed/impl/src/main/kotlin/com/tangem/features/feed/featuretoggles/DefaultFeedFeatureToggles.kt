package com.tangem.features.feed.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.feed.FeedFeatureToggles
import javax.inject.Inject

internal class DefaultFeedFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : FeedFeatureToggles {

    override val isNewShtorkaEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1608_NEW_SHTORKA_ENABLED)
}