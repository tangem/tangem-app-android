package com.tangem.data.sociallogin

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import javax.inject.Inject

internal class DefaultSocialLoginFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : SocialLoginFeatureToggles {

    override val isSocialLoginEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1555_SOCIAL_LOGIN_ENABLED)
}