package com.tangem.data.sociallogin

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import com.tangem.lib.auth.AuthFeatureToggles
import javax.inject.Inject

internal class DefaultSocialLoginFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
    private val authFeatureToggles: AuthFeatureToggles,
) : SocialLoginFeatureToggles {

    /** FR-07: without mobile authentication the device key is a stub and every backend call fails */
    override val isSocialLoginEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1555_SOCIAL_LOGIN_ENABLED) &&
            authFeatureToggles.isBackendAuthenticationEnabled
}