package com.tangem.features.onboarding.v2

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultOnboardingV2FeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : OnboardingV2FeatureToggles {
    override val isAddressSyncEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.ADDRESS_SYNC_ENABLED)

    override val isCardLinkedStatusUpdateV2Enabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1586_CARD_LINKED_STATUS_UPDATE_V2)
}