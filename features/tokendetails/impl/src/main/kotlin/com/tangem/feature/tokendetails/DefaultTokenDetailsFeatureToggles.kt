package com.tangem.feature.tokendetails

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.tokendetails.TokenDetailsFeatureToggles
import javax.inject.Inject

internal class DefaultTokenDetailsFeatureToggles @Inject constructor(
    featureTogglesManager: FeatureTogglesManager,
) : TokenDetailsFeatureToggles {

    override val isQuickTopUpEnabled: Boolean = featureTogglesManager.isFeatureEnabled(
        toggle = FeatureToggles.AND_15258_QUICK_TOP_UP_ENABLED,
    )
}