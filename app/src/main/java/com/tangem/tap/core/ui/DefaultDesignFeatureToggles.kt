package com.tangem.tap.core.ui

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.ui.DesignFeatureToggles
import javax.inject.Inject

class DefaultDesignFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : DesignFeatureToggles {

    override val isRedesignEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.APP_REDESIGN_ENABLED)
}