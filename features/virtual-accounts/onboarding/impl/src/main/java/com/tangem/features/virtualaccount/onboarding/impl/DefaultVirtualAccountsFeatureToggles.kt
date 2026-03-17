package com.tangem.features.virtualaccount.onboarding.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.virtualaccount.onboarding.api.VirtualAccountsFeatureToggles
import javax.inject.Inject

internal class DefaultVirtualAccountsFeatureToggles
@Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : VirtualAccountsFeatureToggles {

    override val isVirtualAccountsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.VIRTUAL_ACCOUNTS_ENABLED)
}