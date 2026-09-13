package com.tangem.features.introduction.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.introduction.IntroductionFeatureToggles

internal class DefaultIntroductionFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : IntroductionFeatureToggles {

    override val isIntroductionRedesignEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.TWI_1356_INTRODUCTION_REDESIGN_ENABLED)
}