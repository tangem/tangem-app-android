package com.tangem.features.onboarding.usedcard

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultUsedCardOnboardingFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : UsedCardOnboardingFeatureToggles {
    override val isUsedCardOnboardingEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("USED_CARD_ONBOARDING_ENABLED")
}