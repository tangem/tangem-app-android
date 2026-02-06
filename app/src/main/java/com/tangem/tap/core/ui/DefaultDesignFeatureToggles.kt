package com.tangem.tap.core.ui

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.ui.DesignFeatureToggles
import javax.inject.Inject

class DefaultDesignFeatureToggles @Inject constructor(
    featureTogglesManager: FeatureTogglesManager,
) : DesignFeatureToggles {
    override val isRedesignEnabled: Boolean = featureTogglesManager.isFeatureEnabled("APP_REDESIGN_ENABLED")
}