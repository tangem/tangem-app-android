package com.tangem.tap.features.details.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

internal class DefaultDetailsFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : DetailsFeatureToggles {

    override val isRedesignedAppCurrencySelectorEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "REDESIGNED_APP_CURRENCY_SELECTOR_ENABLED")
}