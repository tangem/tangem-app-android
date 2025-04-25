package com.tangem.features.onboarding.v2

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultOnboardingV2FeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : OnboardingV2FeatureToggles {
    override val isVisaOnboardingEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("VISA_ONBOARDING_ENABLED")
}