package com.tangem.lib.auth

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultAuthFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : AuthFeatureToggles {

    override val isBackendAuthenticationEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15438_BACKEND_AUTHENTICATION_ENABLED)
}