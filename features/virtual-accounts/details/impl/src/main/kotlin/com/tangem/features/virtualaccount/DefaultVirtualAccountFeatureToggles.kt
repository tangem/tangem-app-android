package com.tangem.features.virtualaccount

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultVirtualAccountFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : VirtualAccountFeatureToggles {
    override val isVirtualAccountsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.VIRTUAL_ACCOUNTS_ENABLED)
}