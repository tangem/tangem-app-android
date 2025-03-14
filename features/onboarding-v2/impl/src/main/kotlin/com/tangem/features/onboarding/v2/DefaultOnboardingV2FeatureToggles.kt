package com.tangem.features.onboarding.v2

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultOnboardingV2FeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : OnboardingV2FeatureToggles {
    override val isOnboardingV2Enabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("ONBOARDING_CODE_REFACTORING_ENABLED")
    override val isVisaOnboardingEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("VISA_ONBOARDING_ENABLED")
    override val isNoteRefactoringEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("NOTE_REFACTORING_ENABLED")
}