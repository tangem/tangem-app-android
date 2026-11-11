package com.tangem.features.onramp.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultOnrampFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : OnrampFeatureToggles {

    override val isCountryByIpEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_18954_ONRAMP_COUNTRY_BY_IP_ENABLED)
}