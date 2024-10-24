package com.tangem.feature.onboarding.legacy.featuretoggle

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultOnboardingLegacyFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : OnboardingLegacyFeatureToggles {
    override val isRewrittenOnboardingCodeInUse: Boolean
        get() = featureTogglesManager.isFeatureEnabled("REWRITTEN_ONBOARDING_CODE")
}
