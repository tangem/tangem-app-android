package com.tangem.features.onboarding.v2

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultOnboardingV2FeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : OnboardingV2FeatureToggles {
    override val isRewrittenOnboardingCodeInUse: Boolean
        get() = featureTogglesManager.isFeatureEnabled("ONBOARDING_CODE_REFACTORING_ENABLED")
}
